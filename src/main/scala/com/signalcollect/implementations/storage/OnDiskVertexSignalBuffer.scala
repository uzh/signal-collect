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

import com.signalcollect.interfaces.VertexSignalBuffer
import com.signalcollect.interfaces.SignalMessage
import com.sleepycat.je._
import java.io.File
import com.signalcollect.implementations.serialization._
import scala.collection.mutable.ArrayBuffer

class OnDiskVertexSignalBuffer(envFolderPath: String = "sc_toCollect") extends VertexSignalBuffer {

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

  def addSignal(signal: SignalMessage[_, _, _]) {
    val key = new DatabaseEntry(DefaultSerializer.write(signal.targetId))
    var value = new DatabaseEntry()
    var signals = new ArrayBuffer[SignalMessage[_, _, _]]()
    if (db.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      signals = DefaultSerializer.read(value.getData())
    } else {
      count += 1
    }
    signals.append(signal)
    val updatedValue = new DatabaseEntry(DefaultSerializer.write(signals))
    db.put(null, key, updatedValue)
  }

  def addVertex(vertexId: Any) {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    var value = new DatabaseEntry()

    if (!(db.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS)) {
      val signals = new ArrayBuffer[SignalMessage[_, _, _]]()
      val updatedValue = new DatabaseEntry(DefaultSerializer.write(signals))
      db.put(null, key, updatedValue)
      count += 1
    }
  }

  def remove(vertexId: Any) {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    db.delete(null, key)
    count -= 1
  }

  def size: Int = count
  def isEmpty = count == 0
  def foreach[U](f: (Any, Iterable[SignalMessage[_, _, _]]) => U, removeAfterProcessing: Boolean, breakCondition: () => Boolean = () => false) = {
    cursor = db.openCursor(null, CursorConfig.DEFAULT)
    var key = new DatabaseEntry()
    var value = new DatabaseEntry()
    if (cursor.getFirst(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      val signals: ArrayBuffer[SignalMessage[_, _, _]] = DefaultSerializer.read(value.getData())
      val vertexId: Int = DefaultSerializer.read(key.getData())
      f(vertexId, signals)
      count -= 1
      if (removeAfterProcessing) {
        cursor.delete()
      }
    }
    while (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS && !breakCondition()) {
      val signals: ArrayBuffer[SignalMessage[_, _, _]] = DefaultSerializer.read(value.getData())
      val vertexId: Int = DefaultSerializer.read(key.getData())
      f(vertexId, signals)
      count -= 1
      if (removeAfterProcessing) {
        cursor.delete()
      }
    }
    cursor.close
    this.size == 0
  }
  def cleanUp = {
  }
}

trait OnDiskSignalBuffer extends DefaultStorage {
  override protected def vertexSignalFactory = {
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new OnDiskVertexSignalBuffer(torqueTempFolder.getAbsolutePath + "/sc-berkeley")
      } else {
        new OnDiskVertexSignalBuffer("sc-berkeley")
      }
    } else {
      new OnDiskVertexSignalBuffer("sc-berkeley")
    }
  }
}