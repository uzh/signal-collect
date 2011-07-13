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

package signalcollect.implementations.storage

import scala.collection.mutable.LinkedHashMap
import signalcollect.interfaces._
import scala.collection.mutable.LinkedHashMap
import scala.concurrent.Lock

/**
 *  Caches Vertices in the store according to a least recently used (LRU) policy
 */
class LRUVertexCache(persistentStorageFactory: Storage => VertexStore,
  storage: Storage,
  var capacity: Int = Int.MaxValue,
  inMemoryRatio: Float = 0.5f) extends VertexStore {

  protected val CACHING_THRESHOLD = inMemoryRatio
  protected lazy val persistentStore: VertexStore = persistentStorageFactory(storage)
  protected val cache = new LRUMap[Any, Vertex[_, _]](persistentStore, capacity)

  def get(id: Any): Vertex[_, _] = {
    val result = cache.get(id)
    if (result != None) {
      result.get
    } else {
      persistentStore.get(id)
    }
  }

  def put(vertex: Vertex[_, _]) = {
    if (cache.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false // Vertex already stored
    } else if (cache.size < capacity) {
      vertex.setMessageBus(storage.getMessageBus)
      cache.put(vertex.id, vertex)
      storage.toCollect.add(vertex.id)
      storage.toSignal.add(vertex.id)
      var usedMemory = Runtime.getRuntime().totalMemory.asInstanceOf[Float] - Runtime.getRuntime().freeMemory
      if ((usedMemory / Runtime.getRuntime().maxMemory) > CACHING_THRESHOLD) {
        capacity = cache.size
      }
      true
    } else {
      persistentStore.put(vertex)
    }
  }

  def remove(id: Any) {
    if (cache.contains(id)) {
      cache.remove(id)
      storage.toCollect.remove(id)
      storage.toSignal.remove(id)
    } else {
      persistentStore.remove(id)
    }
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) {
    if (!cache.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }

  def size: Long = cache.size + persistentStore.size

  def foreach[U](f: (Vertex[_, _]) => U) {
    cache.applyFunction(f)
    persistentStore.foreach(f)
  }

  def cleanUp {
    persistentStore.cleanUp
    cache.clear
  }
}

/**
 * Least Recently Used Map
 *
 * Keeps entries in the order of the last access (insertions also count as an access)
 */
class LRUMap[A, B](storage: VertexStore, maxCapacity: Int) extends LinkedHashMap[A, B] {

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

  def serializeLRU {
    val vertexToSerialize = this.firstEntry.value
    storage.put(vertexToSerialize.asInstanceOf[Vertex[_, _]]);
    remove(firstEntry.key)
  }

  def updateUsage(key: A) {
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

  def applyFunction[U](f: (Vertex[_, _]) => U) {
    foreachEntry(entry => f(entry.value.asInstanceOf[Vertex[_, _]]))
  }

}

trait LRUCache extends DefaultStorage {
  def berkeleyDBFactory(storage: Storage) = new BerkeleyDBStorage(storage, ".")
  override protected def vertexStoreFactory = new LRUVertexCache(berkeleyDBFactory, this)
}