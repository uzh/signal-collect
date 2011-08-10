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

/**
 * On-disk version of the collection that holds all undelivered signals for a vertex for processing them and to indicate which vertices have to collect.
 * This implementation uses Berkeley DB as its storage back end.
 *
 * Since this implementation has to store all the information on disk and serialize them using this class instead of the
 * in-memory version will slow down computation considerably and therefore the usage of this collection should be
 * avoided if ever possible.
 *
 * @param envFolderPath the path where the database environment should be stored.
 */
class OnDiskVertexSignalBuffer(envFolderPath: String = "sc_toCollect") extends VertexSignalBuffer {

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
   * Adds a new entry for the recipient of that signal to the database if the recipient is not already stored. That entry contains the signal that is provided.
   * If the recipent already has signals waiting in the buffer the new signal is added to the other ones. This involves deserializing the old signals and serializing the new collection.
   * 
   *  @param signal the signal to add to the signal buffer for a recipient.
   */
  def addSignal(signal: SignalMessage[_, _, _]) {
    val key = new DatabaseEntry(DefaultSerializer.write(signal.edgeId.targetId))
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

  /**
   * If the recipient is not contained in the database a new entry is created with an empty signal list 
   * so that the vertex with that ID is still handled in the next processing phase even though no
   * no signals are buffered.
   * 
   * @param vertexId the ID of the vertex to add to the database
   */
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

  /**
   * Deletes an entry for a vertex in the database.
   */
  def remove(vertexId: Any) {
    val key = new DatabaseEntry(DefaultSerializer.write(vertexId))
    db.delete(null, key)
    count -= 1
  }

  /**
   * Returns the number of vertices that are stored in the database
   */
  def size: Int = count
  def isEmpty = count == 0
  
  /**
   * Applies the specified function to all entries in the database.
   * Iterating through the collection can optionally be escaped.
   * 
   * @param f the function to apply to each vertex referenced by an entry in the database
   * @param removeAfterProcessing indicates whether an entry should be removed after iterating over it
   * @param breakCondition if this function returns true the iteration is escaped even though not all entries were processed
   * 
   * @return Iteration over all entries was completed.
   */
  def foreach[U](f: (Any, Iterable[SignalMessage[_, _, _]]) => U, removeAfterProcessing: Boolean, breakCondition: () => Boolean = () => false): Boolean = {
    val cursor = db.openCursor(null, CursorConfig.DEFAULT)
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
    db.close
    if (envFolder.exists() && envFolder.isDirectory) {
      val filesInFolder = envFolder.listFiles
      filesInFolder.foreach(file => file.delete)
    }
    envFolder.delete
  }
}

trait OnDiskSignalBuffer extends DefaultStorage {
  override protected def vertexSignalFactory = {
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        new OnDiskVertexSignalBuffer(torqueTempFolder.getAbsolutePath + "/sc_toCollect")
      } else {
        new OnDiskVertexSignalBuffer("sc_toCollect")
      }
    } else {
      new OnDiskVertexSignalBuffer("sc_toCollect")
    }
  }
}