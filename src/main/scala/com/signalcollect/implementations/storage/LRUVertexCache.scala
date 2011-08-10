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

import scala.collection.mutable.LinkedHashMap
import com.signalcollect.interfaces._
import scala.collection.mutable.LinkedHashMap
import java.io.File

/**
 *  Caches Vertices in the store according to a least recently used (LRU) policy
 *  It allows to cache any storage back end with a in-memory buffer.
 *  
 *  @param persistentStorageFactory function that creates the storage that should be buffered
 *  @param storage the storage object that also contains the toSignal and toCollect collections.
 *  @param capacity optionally limits the number of entries that may be cached
 *  @param inMemoryRatio the percentage of main memory that is allowed to be used.
 */
class LRUVertexCache(persistentStorageFactory: Storage => VertexStore,
  storage: Storage,
  var capacity: Int = Int.MaxValue,
  inMemoryRatio: Option[Float] = None) extends VertexStore {

  protected val CACHING_THRESHOLD = inMemoryRatio
  protected lazy val persistentStore: VertexStore = persistentStorageFactory(storage)
  protected val cache = new LRUMap[Any, Vertex](persistentStore, capacity)

  /**
   * Returns the vertex with the specified Id from the storage
   * 
   * @return the vertex with that id or null if the store does not contain a vertex with this id.
   */
  def get(id: Any): Vertex = {
    val result = cache.get(id)
    if (result != None) {
      result.get
    } else {
      persistentStore.get(id)
    }
  }

  /**
   * Adds a vertex to the cache if the cache is not exhausted or else adds it to the persistent storage.
   * 
   * @return true if the insertion was successful or false if the storage already contains a vertex with this id.
   */
  def put(vertex: Vertex): Boolean = {
    if (cache.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false // Vertex already stored
    } else if (cache.size < capacity) {
      cache.put(vertex.id, vertex)
      var usedMemory = Runtime.getRuntime().totalMemory.asInstanceOf[Float] - Runtime.getRuntime().freeMemory
      if (CACHING_THRESHOLD.isDefined && (usedMemory / Runtime.getRuntime().maxMemory) > CACHING_THRESHOLD.get) {
        capacity = cache.size
        cache.setCapacity(capacity)
      }
      true
    } else {
      persistentStore.put(vertex)
    }
  }

  /**
   * Removes the vertex with the specified id from the storage
   * 
   * @param the id of the vertex that should be removed.
   */
  def remove(id: Any) {
    if (cache.contains(id)) {
      cache.remove(id)
      storage.toCollect.remove(id)
      storage.toSignal.remove(id)
    } else {
      persistentStore.remove(id)
    }
  }

  /**
   * Updates the state of the vertex if the vertex is not contained in the cache.
   * If the vertex is contained in the cache it does not have to be retained since it is held in memory.
   */
  def updateStateOfVertex(vertex: Vertex) {
    if (!cache.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }

  def size: Long = persistentStore.size + cache.size

  /**
   * Applies the function to each vertex in the cache and in the cached storage.
   * 
   * @param f the function to apply
   */
  def foreach[U](f: (Vertex) => U) {
    cache.applyFunction(f)
    persistentStore.foreach(f)
  }

  /**
   * removes all entries from the cache as well as from the cached storage
   */
  def cleanUp {
    persistentStore.cleanUp
    cache.clear
  }
}

/**
 * Least Recently Used Map data structure that, keeps entries in the order of the last access (insertions also count as an access)
 * when the maximum capacity is exceeded the supernumerous entries are stored to the other storage entity.
 * 
 * @param storage the vertex storage to hold all the vertices that do not fit into the storage any more.
 * @maxCapcity = the maximum capacity of the storage
 */
class LRUMap[A, B](storage: VertexStore, var maxCapacity: Int) extends LinkedHashMap[A, B] {

  override def put(key: A, value: B): Option[B] = {
    val res = super.put(key, value)
    if (this.size > maxCapacity) {
      serializeLRU
    }
    res
  }

  override def get(key: A): Option[B] = {
    val res = super.get(key)
    if (res != None) {
      updateUsage(key)
    }
    res
  }

  /**
   * Removes an entry from the cache and stores it in the larger storage.
   */
  protected def serializeLRU {
    val vertexToSerialize = this.firstEntry.value
    storage.put(vertexToSerialize.asInstanceOf[Vertex]);
    remove(firstEntry.key)
  }

  /**
   * Corrects the order of the linked list on update
   */
  protected def updateUsage(key: A) {
    val entry = this.findEntry(key)
    if (entry == this.lastEntry) {
      //No need to move it back
    } else {
      //remove entry
      if (entry.earlier != null) {
        entry.earlier.later = entry.later
      } else {
        this.firstEntry = entry.later
      }
      if (entry.later != null) {
        entry.later.earlier = entry.earlier
      }
      //enqueue entry again
      if (firstEntry == null) firstEntry = entry
      else {
        lastEntry.later = entry
        entry.earlier = lastEntry
      }
      lastEntry = entry
    }
  }

  def applyFunction[U](f: (Vertex) => U) {
    foreachEntry(entry => f(entry.value.asInstanceOf[Vertex]))
  }
  
  /**
   * Resets the capacity of the cache and removes supernumerous entries from the cache
   */
  def setCapacity(capacity: Int) {
    while(maxCapacity>capacity) {
      serializeLRU
      maxCapacity-=1
    }
    maxCapacity=capacity //in case the capacity increased
  }

}

/**
 * Allows the Default storage to use the a cached version of Berkeley DB as its storage back end. 
 * However Berkeley DB has its own caching strategy that is more efficient in most scenarios.
 */
trait CachedBerkeley extends DefaultStorage {

  def berkeleyDBFactory(storage: Storage) = {
    var folderPath: String = "sc-berkeley"
    val userName = System.getenv("USER")
    val jobId = System.getenv("PBS_JOBID")
    if (userName != null && jobId != null) {
      val torqueTempFolder = new File("/home/torque/tmp/" + userName + "." + jobId)
      if (torqueTempFolder.exists && torqueTempFolder.isDirectory) {
        folderPath = torqueTempFolder.getAbsolutePath + "/sc-berkeley"
      }
    }
    new BerkeleyDBStorage(storage, folderPath)
  }
  override protected def vertexStoreFactory = new LRUVertexCache(berkeleyDBFactory, this, 4)
}