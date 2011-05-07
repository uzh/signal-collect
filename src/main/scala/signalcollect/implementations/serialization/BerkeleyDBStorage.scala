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

package signalcollect.implementations.serialization

import com.sleepycat.persist.model.KeyField
import com.sleepycat.persist.model.PrimaryKey
import com.sleepycat.persist.model.Entity
import signalcollect.interfaces._
import java.io.File
import com.sleepycat.je.{ Environment, EnvironmentConfig }
import com.sleepycat.persist.{ EntityCursor, EntityStore, StoreConfig }
import scala.concurrent.Lock

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
class BerkeleyDBStorage(storage: Storage, envFolderPath: String = "/tmp/") extends VertexStore with DefaultSerializer {

  val messageBus = storage.getMessageBus
  var count = 0l
  val lock = new Lock()

  /* Open the JE Environment. */
  val envConfig = new EnvironmentConfig()
  envConfig.setAllowCreate(true)
  envConfig.setLocking(false)
  
  /* Create folder for environment */
  var envFolder = new File(envFolderPath)
  if (!envFolder.exists) {
    val folderCreated = new File(envFolderPath).mkdir
    if (!folderCreated) {
      System.err.println("Couldn't create folder: " + envFolderPath + " for Berkeley DB.");
      System.err.println("Specify another folder or try to create it manually");
      System.exit(-1);
    }
    envFolder = new File(envFolderPath)
  }
  val env = new Environment(envFolder, envConfig)

  /* Open the DPL Store. */
  val storeConfig = new StoreConfig()
  storeConfig.setAllowCreate(true)
  val store = new EntityStore(env, getRandomString("sc", 16), storeConfig)

  val primaryIndex = store.getPrimaryIndex(classOf[String], classOf[Vertex2EntityAdapter])

  def get(id: Any): Vertex[_, _] = {
	lock.acquire
    val storedObject = primaryIndex.get(id.toString)
    lock.release
    if (storedObject != null) {
      var vertex = read(storedObject.vertex).asInstanceOf[Vertex[_, _]]
      vertex.setMessageBus(messageBus)
      vertex
    }
    else {
    	null
    }

  }

  def put(vertex: Vertex[_, _]): Boolean = {
    if (primaryIndex.get(vertex.id.toString) == null) {
      primaryIndex.put(new Vertex2EntityAdapter(vertex.id.toString, write(vertex)))
      storage.toCollect += vertex.id
      storage.toSignal += vertex.id
      count += 1l

      true
    } else {
      false
    }
  }
  
  def remove(id: Any) = {
    primaryIndex.delete(id.toString)
    count -= 1
  }
  
  def updateStateOfVertex(vertex: Vertex[_, _]) = {
	lock.acquire
    primaryIndex.put(new Vertex2EntityAdapter(vertex.id.toString, write(vertex)))
    lock.release
  }
  
  def size: Long = count
  
  def foreach[U](f: (Vertex[_, _]) => U) {
    val cursor = primaryIndex.entities
    var currentElement = cursor.first
    while (currentElement != null) {
      val vertex = read(currentElement.vertex).asInstanceOf[Vertex[_, _]]
      f(vertex)
      currentElement = cursor.next
    }
  }
}

/**
 * To allow mixing-in this storage implementation into a more general storage implementation
 */
trait BerkDBJE extends DefaultStorage {
  override protected def vertexStoreFactory = new BerkeleyDBStorage(this, getRandomString("/tmp/", 3))
}