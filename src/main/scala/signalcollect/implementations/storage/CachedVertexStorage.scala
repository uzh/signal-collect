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

import scala.collection.mutable.SynchronizedPriorityQueue
import signalcollect.interfaces._
import scala.concurrent.Lock

class CachedVertexStorage(persistentStorageFactory: Storage => VertexStore, storage: Storage) extends VertexStore {

  val CACHING_THRESHOLD = 0.50 // % of main memory that can be used by the in-memory vertex store

  val lock = new Lock()

  val inMemoryCache = new InMemoryStorage(storage) with CacheFunctionality
  lazy val persistentStore: VertexStore = persistentStorageFactory(storage) // Storage that should be cached

  def get(id: Any): Vertex[_, _] = {
    if (inMemoryCache.contains(id)) {
      lock.acquire
      val vertex = inMemoryCache.get(id)
      lock.release
      vertex.setScoreCache(vertex.scoreCache+1)
      vertex
    } else {
      val vertex = persistentStore.get(id)
      if(vertex != null) {
    	  vertex.setScoreCache(vertex.scoreCache+1)
    	  lock.acquire
    	  inMemoryCache.cache(vertex) 
    	  lock.release
      }
      vertex
    }
  }

  def put(vertex: Vertex[_, _]): Boolean = {
    if (inMemoryCache.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false // Vertex already stored
    } else if (!cacheRatioReached) {
      lock.acquire
      inMemoryCache.put(vertex)
      lock.release
      true
    } else {
      persistentStore.put(vertex)
      true
    }
  }

  def remove(id: Any) {
    if (inMemoryCache.contains(id)) {
      inMemoryCache.remove(id)
    }
    persistentStore.remove(id)
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) {
    if (!inMemoryCache.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }
  def size: Long = inMemoryCache.size + persistentStore.size

  def foreach[U](f: (Vertex[_, _]) => U) {
    inMemoryCache.foreach(f)
    persistentStore.foreach(f)
  }

  def cacheRatioReached = ((Runtime.getRuntime().totalMemory.asInstanceOf[Float]-Runtime.getRuntime().freeMemory) / Runtime.getRuntime().maxMemory) > CACHING_THRESHOLD
}

trait CachedDB extends DefaultStorage {
  def berkeleyDBFactory(storage: Storage) = new BerkeleyDBStorage(storage, getRandomString("/tmp/", 3))
  override protected def vertexStoreFactory = new CachedVertexStorage(berkeleyDBFactory, this)
}

trait CacheFunctionality extends InMemoryStorage {
  val vertexQueue = new SynchronizedPriorityQueue[Any]()(lowestCacheScoreOrdering) // Replacement priority of the cache

  def contains(id: Any) = vertexMap.containsKey(id)
  
  def cache(vertex: Vertex[_, _]) {
    if(!vertexQueue.isEmpty && vertex.scoreSignal>1.05*get(vertexQueue.head).scoreSignal) { //new vertex needs to be more cache-worthy than the current cache to prevent too much swapping in and out
      val oldId = vertexQueue.dequeue
      val oldVertex = get(oldId)
      remove(oldId)
      vertexQueue.enqueue(vertex.id)
      put(vertex)
    }
  }
  
  override def put(vertex: Vertex[_, _]): Boolean = {
    val success = super.put(vertex)
    if(success) {
      vertexQueue.enqueue(vertex.id)
    }
    success
  }
  
  // Caching Strategies
  def highestCacheScoreOrdering = new Ordering[Any] {
    def compare(v1: Any, v2: Any): Int = get(v1).scoreCache.compare(get(v2).scoreCache)
  }
  
  def lowestCacheScoreOrdering = new Ordering[Any] {
    def compare(v1: Any, v2: Any): Int = get(v2).scoreCache.compare(get(v1).scoreCache)
  }

}