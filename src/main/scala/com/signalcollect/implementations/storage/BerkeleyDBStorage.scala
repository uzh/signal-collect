/*
 *  @author Daniel Strebel
 *
 *  Copyright 2011 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.signalcollect.implementations.storage

import com.sleepycat.persist.model.KeyField
import com.sleepycat.persist.model.PrimaryKey
import com.sleepycat.persist.model.Entity
import com.signalcollect.interfaces._
import java.io.File
import com.sleepycat.je.{ Environment, EnvironmentConfig }
import com.sleepycat.persist.{ EntityCursor, EntityStore, StoreConfig }
import scala.concurrent.Lock
import com.signalcollect.implementations.serialization._
import com.signalcollect._

/**
 * Wrapper for vertices to be compatible with Berkeley DB JE
 *
 * @Precondition Parameter idParam is unique and consistent within a vertex storage
 */
@Entity
class Vertex2EntityAdapter(idParam: String, vertexParam: Array[Byte]) {

  @PrimaryKey
  var id: String = idParam
  var vertex = vertexParam

  def this() = this(null, null) // default ctor for unmarshalling

}

/**
 * Saves Vertices in a Berlekey DB JE database
 *
 * For more information about Berkeley DB JE visit:
 * @See <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html">Oracle Berkeley DB Java Edition product page</a>
 *
 * @SeeAlso Persistency of the database is currently handled by the worker class
 *
 * @param storage 	provides the messageBus and pointers to the collection that hold the toSignal and toCollect Lists
 * @param envFolder	Make sure this folder actually exists by typing "mkdir /tmp" or set parameter to an existing folder
 */
class BerkeleyDBStorage(storage: Storage, envFolderPath: String = "sc_vertices") extends VertexStore {

  var count = 0l
  val serializer = storage.serializer

  /* Open the JE Environment. */
  val envConfig = new EnvironmentConfig()
  envConfig.setAllowCreate(true)
  envConfig.setLocking(false)
  envConfig.setCachePercent(1)

  /* Create folder for environment */
  var envFolder = new File(envFolderPath)
  if (!envFolder.exists) {
    val folderCreated = new File(envFolderPath).mkdir
    var envFolder = new File(envFolderPath)
    if (!envFolder.exists()) {
      System.err.println("Couldn't create folder: " + envFolder.getAbsolutePath + " for Berkeley DB.");
      System.err.println("Specify another folder or try to create it manually");
      System.exit(-1);
    }
    envFolder = new File(envFolderPath)
  }
  val env = new Environment(envFolder, envConfig)

  /* Open the DPL Entity Store and open an index for it */
  val storeConfig = new StoreConfig()
  storeConfig.setAllowCreate(true)
  //storeConfig.setDeferredWrite(true) //Use this to not write the entries persistently to disk e.g. at load time
  val store = new EntityStore(env, RandomString("sc", 12), storeConfig)
  val primaryIndex = store.getPrimaryIndex(classOf[String], classOf[Vertex2EntityAdapter])

  /**
   * Returns a vertex from the store that has the specified id.
   * 
   * @param id the ID of the vertex to retrieve
   * @return the vertex object or null if the vertex is not contained in the store
   */
  def get(id: Any): Vertex = {
    val storedObject = primaryIndex.get(id.toString)
    if (storedObject != null) {
      var vertex: Vertex = null
      vertex = serializer.read(storedObject.vertex)
      vertex
    } else {
      null
    }

  }

  /**
   * Adds a vertex to the store if the store does not contain already a vertex with the same id
   * 
   * @param the vertex that needs to be added to the storage.
   * @return true if the vertex was successfully inserted to the storage or false if the storage already contains a vertex with the same id.
   */
  def put(vertex: Vertex): Boolean = {  
   val insertSuccessful = primaryIndex.putNoOverwrite(new Vertex2EntityAdapter(vertex.id.toString, serializer.write(vertex)))
   if(insertSuccessful) {
      count += 1l
   }
   insertSuccessful
  }

  /**
   * Removes a vertex from the storage and deletes its entries in the responsible to signal and to handle collections.
   * 
   * @param the id of the vertex to remove
   */
  def remove(id: Any) = {
    storage.toCollect.remove(id)
    storage.toSignal.remove(id)
    primaryIndex.delete(id.toString)
    count -= 1
  }

  /**
   * Persistently writes the current state of the vertex to the storage so that changes will be reflected when
   * it is retrieved for the next time.
   * 
   * @param vertex the vertex that has to be written back to the storage
   */
  def updateStateOfVertex(vertex: Vertex) = {
    primaryIndex.put(new Vertex2EntityAdapter(vertex.id.toString, serializer.write(vertex)))
  }

  /**
   * Number of vertices in the storage
   * 
   * @return number of vertices
   */
  def size: Long = count

  /**
   * Applies the specified function to each vertex in the storage.
   * This involves reading a vertex from disk, deserialization, serialization and retaining its changed state and is therefore
   * a rather expensive operation especially if a large number of vertices is contained in the store. 
   * 
   * @param f the function that should be applied to each function in the storage
   */
  def foreach[U](f: (Vertex) => U) {
    val cursor = primaryIndex.entities
    var currentElement = cursor.first
    while (currentElement != null) {
      val vertex = serializer.read(currentElement.vertex).asInstanceOf[Vertex]
      f(vertex)
      updateStateOfVertex(vertex)
      currentElement = cursor.next
    }
  }

  /**
   * Closes the EntityStore add removes all the data files from the folder on disk. 
   */
  def cleanUp {
    store.close
    storage.synchronized {
      if (envFolder.exists() && envFolder.isDirectory) {
        val filesInFolder = envFolder.listFiles
        filesInFolder.foreach(file => file.delete)
      }
      envFolder.delete
    }
  }
}

/**
 * To allow mixing-in this storage implementation into a more general storage implementation
 * If the execution takes place on a torque cluster machine, then the temporary folder of that torque job "/home/torque/tmp/$USER.$PBS_JOBID" is used.
 * Else the environment is created in the current directory of that programs execution.
 */
trait BerkDBJE extends DefaultStorage with Serializable {

  override protected def vertexStoreFactory = {
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new BerkeleyDBStorage(this, torqueTempFolder.getAbsolutePath + "/sc-berkeley")
      } else {
        new BerkeleyDBStorage(this, "sc-berkeley")
      }
    } else {
      new BerkeleyDBStorage(this, "sc-berkeley")
    }
  }
} 