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

import signalcollect.interfaces._
import scala.concurrent.Lock
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Can be used to cache vertices based on some scoring function.
 * The vertices with the highest caching scores should remain in main memory
 *
 * @param scoreUpdate used to update the score each time that vertex is requested from the storage
 * @param inMemoryRatio percentage of main memory usage from where the storage should start to use the secondary storage
 */
class ScoredVertexCache(persistentStorageFactory: Storage => VertexStore,
  storage: Storage,
  scoreUpdate: Double => Double,
  inMemoryRatio: Float = 0.5f) extends VertexStore {

  protected val CACHING_THRESHOLD = inMemoryRatio
  protected var cacheRatioReached = false

  protected val cacheScores = new HashMap[Any, Double]()
  protected var averageScore = 0.0;
  private val lock = new Lock()

  protected val inMemoryCache = new InMemoryCache(storage)
  protected lazy val persistentStore: VertexStore = persistentStorageFactory(storage) // Storage that should be cached

  def get(id: Any): Vertex[_, _] = {
    //Update the cache scores
    val oldCacheScore = cacheScores.get(id)
    val newCacheScore = scoreUpdate(oldCacheScore)
    cacheScores.put(id, newCacheScore)
    averageScore = ((averageScore * cacheScores.size) - oldCacheScore + newCacheScore) / cacheScores.size

    if (inMemoryCache.contains(id)) {
      lock.acquire
      val vertex = inMemoryCache.get(id)
      lock.release
      vertex
    } else {
      val vertex = persistentStore.get(id)
      if (vertex != null) {
        lock.acquire
        inMemoryCache.cache(vertex)
        lock.release
      }
      vertex
    }
  }

  def put(vertex: Vertex[_, _]): Boolean = {
    cacheScores.put(vertex.id, 0.0)
    if (inMemoryCache.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false // Vertex already stored
    } else if (!cacheRatioReached) {
      lock.acquire
      inMemoryCache.put(vertex)
      lock.release
      var usedMemory = Runtime.getRuntime().totalMemory.asInstanceOf[Float] - Runtime.getRuntime().freeMemory
      if ((usedMemory / Runtime.getRuntime().maxMemory) > CACHING_THRESHOLD) {
        cacheRatioReached = true
        inMemoryCache.maxSize = inMemoryCache.size
      }
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
    var usedMemory = Runtime.getRuntime().totalMemory.asInstanceOf[Float] - Runtime.getRuntime().freeMemory
    if ((usedMemory / Runtime.getRuntime().maxMemory) < CACHING_THRESHOLD) {
      cacheRatioReached = false
    }
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) {
    if (!inMemoryCache.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }
  def size = cacheScores.size

  def foreach[U](f: (Vertex[_, _]) => U) {
    inMemoryCache.foreach(f)
    persistentStore.foreach(f)
  }

  protected class InMemoryCache(storage: Storage) extends VertexStore {
    var maxSize = Long.MaxValue //gets reset if cache ratio is reached
    val messageBus = storage.getMessageBus
    val cachedVertices = new ConcurrentHashMap[Any, Vertex[_, _]](100000, 0.75f, ComputeGraph.defaultNumberOfThreadsUsed)

    def contains(id: Any) = cachedVertices.containsKey(id)

    def cache(vertex: Vertex[_, _]) {
      if (isFull) {
        consolidateCache
      }
    }

    def get(id: Any): Vertex[_, _] = {
      cachedVertices.get(id)
    }

    def put(vertex: Vertex[_, _]): Boolean = {
      if (!cachedVertices.containsKey(vertex.id)) {
        vertex.setMessageBus(messageBus)
        cachedVertices.put(vertex.id, vertex)
        storage.toCollect.add(vertex.id)
        storage.toSignal.add(vertex.id)
        true
      } else
        false
    }
    def remove(id: Any) = {
      cachedVertices.remove(id)
      storage.toCollect.remove(id)
      storage.toSignal.remove(id)
    }

    def updateStateOfVertex(vertex: Vertex[_, _]) = {} // Not needed for in-memory implementation

    def foreach[U](f: (Vertex[_, _]) => U) {
      val it = cachedVertices.values.iterator
      while (it.hasNext) {
        val vertex = it.next
        f(vertex)
      }
    }

    def consolidateCache {
      val it = cachedVertices.values.iterator
      while (it.hasNext) {
        val vertex = it.next
        if (cacheScores.get(vertex.id) < averageScore) {
          persistentStore.put(vertex)
          cachedVertices.remove(vertex.id)
        }
      }
    }

    def size: Long = cachedVertices.size

    def isFull = size > maxSize

  }
}

trait ScoredCache extends DefaultStorage {
  def berkeleyDBFactory(storage: Storage) = new BerkeleyDBStorage(storage, getRandomString("/tmp/", 5))
  override protected def vertexStoreFactory = new ScoredVertexCache(berkeleyDBFactory, this, score => score + 1)
}

