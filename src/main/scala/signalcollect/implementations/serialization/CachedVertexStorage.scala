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

import scala.collection.mutable.Queue
import signalcollect.interfaces._
import scala.concurrent.Lock

class CachedVertexStorage(persistentStorageFactory: Storage => VertexStore, storage: Storage) extends VertexStore {

  val CACHING_THRESHOLD = 0.35 // % of main memory that should NOT be used by the in-memory vertex store

  val lock = new Lock()

  val vertexQueue = new Queue[Any]() // Replacement priority of the cache
  val inMemoryCache: VertexStore = new InMemoryStorage(storage)
  lazy val persistentStore: VertexStore = persistentStorageFactory(storage) // Storage that should be cached

  def get(id: Any): Vertex[_, _] = {
    if (vertexQueue.contains(id)) {
    	lock.acquire
      val vertex = inMemoryCache.get(id)
      lock.release
      vertex
    } else { 
//      lock.acquire
      val vertex = persistentStore.get(id)
//      if (vertex != null && !vertexQueue.isEmpty) {
//        handleCaching(vertex)
//      }
//      lock.release
      vertex
    }
  }

  def put(vertex: Vertex[_, _]): Boolean = {
    if (vertexQueue.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false
    } else if (!cacheRatioReached) {
      vertexQueue.enqueue(vertex.id)
      inMemoryCache.put(vertex)
      true
    } else {
      persistentStore.put(vertex)
      true
    }
  }

  def remove(id: Any) {
    if (vertexQueue.contains(id)) {
      inMemoryCache.remove(id)
    }
    persistentStore.remove(id)
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) {
    if (!vertexQueue.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }
  def size: Long = inMemoryCache.size + persistentStore.size

  def foreach[U](f: (Vertex[_, _]) => U) {
    inMemoryCache.foreach(f)
    persistentStore.foreach(f)
  }

  def cacheRatioReached = (Runtime.getRuntime().freeMemory.asInstanceOf[Float] / Runtime.getRuntime().totalMemory) < CACHING_THRESHOLD

  def handleCaching(vertex: Vertex[_, _]) {
    persistentStore.remove(vertex.id)
    vertexQueue.enqueue(vertex.id)
    inMemoryCache.put(vertex)
    val id = vertexQueue.dequeue
    val surplusVertex = inMemoryCache.get(id)
    inMemoryCache.remove(id)
    persistentStore.put(surplusVertex)
  }
}

trait CachedDB extends DefaultStorage {
  def berkeleyDBFactory(storage: Storage) = new BerkeleyDBStorage(storage)
  override protected def vertexStoreFactory = new CachedVertexStorage(berkeleyDBFactory, this)
}