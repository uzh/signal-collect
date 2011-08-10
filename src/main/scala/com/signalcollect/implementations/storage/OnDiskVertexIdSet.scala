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

import com.sleepycat.je._
import java.io.File
import com.signalcollect.implementations.serialization._
import com.signalcollect.interfaces.VertexIdSet

/**
 * On-disk version of the collection that holds a set of vertex ids for processing them.
 * This implementation uses Berkeley DB to hold the IDs.
 *
 * Since this implementation has to store all the ids on disk and serialize them using this class instead of the
 * in-memory version will slow down computation considerably and therefore the usage of this collection should be
 * avoided if ever possible.
 *
 * @param envFolderPath the path where the database environment should be stored.
 */
class OnDiskVertexIdSet(envFolderPath: String = "sc_toSignal") extends VertexIdSet {

  var count = 0 //counts the number of vertices for which signals are buffered

  /* Create the environment */
  val envConfig = new EnvironmentConfig()
  envConfig.setAllowCreate(true)
  envConfig.setLocking(false)
  envConfig.setCachePercent(10)

  var envFolder = new File(envFolderPath)
  if (!envFolder.exists) {
    val folderCreated = new File(envFolderPath).mkdir
    var tryCount = 0
    var envFolder = new File(envFolderPath)
    if (!envFolder.exists()) {
      System.err.println("Couldn't create folder: " + envFolder.getAbsolutePath + " for Berkeley DB.");
      System.err.println("Specify another folder or try to create it manually");
      System.exit(-1);
    }
    envFolder = new File(envFolderPath)
  }
  val env = new Environment(envFolder, envConfig)

  /* Create the database */
  val dataBaseConfig = new DatabaseConfig
  dataBaseConfig.setAllowCreate(true)
  dataBaseConfig.setTransactional(false)
  val db = env.openDatabase(null, RandomString("toCollect", 4), dataBaseConfig)

  /**
   * Adds a new ID to the data base
   *
   * @param vetexId the ID of the vertex that should be added to the collection.
   */
  def add(vertexId: Any): Unit = {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    var value = new DatabaseEntry(new Array[Byte](0))
    if (!(db.putNoOverwrite(null, key, value) == OperationStatus.KEYEXIST)) {
      count += 1
    }
  }

  /**
   * Removes an ID from the data base
   *
   * @param vertexId the ID of the vertex that should be removed
   */
  def remove(vertexId: Any): Unit = {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    db.delete(null, key)

  }

  def isEmpty: Boolean = count == 0

  def size: Int = count

  /**
   * Applies the specified function to each vertex id and removes the ids if necessary
   *
   * @param f the function to apply to each id
   * @removeAfterProcessing whether the ids should be deleted after they are covered by the function
   */
  def foreach[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    val cursor = db.openCursor(null, CursorConfig.DEFAULT)
    var key = new DatabaseEntry()
    var value = new DatabaseEntry()
    if (cursor.getFirst(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      f(DefaultSerializer.read(key.getData()))
      if (removeAfterProcessing) {
        cursor.delete
        count -= 1
      }
    }
    while (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      f(DefaultSerializer.read(key.getData()))
      if (removeAfterProcessing) {
        cursor.delete
        count -= 1
      }
    }
    cursor.close
  }

  def cleanUp = {
    db.close
    if (envFolder.exists() && envFolder.isDirectory) {
      val filesInFolder = envFolder.listFiles
      filesInFolder.foreach(file => file.delete)
    }
    envFolder.delete
  }
}

/**
 * Allows overriding the default VertexIdSet of a storage implementation.
 * For performance reasons, only override the in-memory VertexIdSet if really necessary 
 */
trait OnDiskIdSet extends DefaultStorage {
  override protected def vertexSetFactory = {
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new OnDiskVertexIdSet(torqueTempFolder.getAbsolutePath + "/sc_toSignal")
      } else {
        new OnDiskVertexIdSet("sc_toSignal")
      }
    } else {
      new OnDiskVertexIdSet("sc_toSignal")
    }
  }
}