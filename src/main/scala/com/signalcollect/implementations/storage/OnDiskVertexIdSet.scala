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


class OnDiskVertexIdSet(envFolderPath: String = "sc_toSignal") extends VertexIdSet {
  
 var count = 0 //counts the number of vertices for which signals are buffered

  /* Open the JE Environment. */
  val envConfig = new EnvironmentConfig()
  envConfig.setAllowCreate(true)
  envConfig.setLocking(false)
  envConfig.setCachePercent(10)

  /* Create folder for environment */
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
  val dataBaseConfig = new DatabaseConfig
  dataBaseConfig.setAllowCreate(true)
  dataBaseConfig.setTransactional(false)
  val db = env.openDatabase(null, RandomString("toCollect", 4), dataBaseConfig)
  var cursor: Cursor = db.openCursor(null, CursorConfig.DEFAULT)
 

  def add(vertexId: Any): Unit = {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    var value = new DatabaseEntry(new Array[Byte](0))
    if(!(db.putNoOverwrite(null, key, value)==OperationStatus.KEYEXIST)) {
      count+=1
    }
  }

  def remove(vertexId: Any): Unit = {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    db.delete(null, key)
    
  }

  def isEmpty: Boolean = count ==0

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
    if(cursor.getFirst(key,value,LockMode.DEFAULT)==OperationStatus.SUCCESS) {
      f(DefaultSerializer.read(key.getData()))
      if(removeAfterProcessing) {
        cursor.delete
        count-=1
      }
    }
    while(cursor.getNext(key,value,LockMode.DEFAULT)==OperationStatus.SUCCESS) {
      f(DefaultSerializer.read(key.getData()))
      if(removeAfterProcessing) {
        cursor.delete
        count-=1
      }
    }
    cursor.close
  }

  def cleanUp = {
  }
}

trait OnDiskIdSet extends DefaultStorage {
  override protected def vertexSetFactory = {
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new OnDiskVertexIdSet(torqueTempFolder.getAbsolutePath + "/sc-berkeley")
      } else {
        new OnDiskVertexIdSet("sc-berkeley")
      }
    } else {
      new OnDiskVertexIdSet("sc-berkeley")
    }
  }
}