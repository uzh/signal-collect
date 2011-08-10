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

import com.signalcollect.interfaces._
import scala.concurrent.Lock
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Can be used to cache vertices based on some scoring function.
 * The vertices with the highest caching scores should remain in main memory
 *
 * @param persistentStorageFactory creates the storage back end that should be cached
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

  /**
   * Returns a vertex from the storage that has the specified id
   *
   * @param id
   */
  def get(id: Any): Vertex = {
    //Update the cache scores
    val oldCacheScore = cacheScores.get(id)
    val newCacheScore = scoreUpdate(oldCacheScore)
    cacheScores.put(id, newCacheScore)
    averageScore = ((averageScore * cacheScores.size) - oldCacheScore + newCacheScore) / cacheScores.size

    //return the vertex
    if (inMemoryCache.contains(id)) {
      inMemoryCache.get(id)
    } else {
      val vertex = persistentStore.get(id)
      if (vertex != null) {
        inMemoryCache.cache(vertex)
      }
      vertex
    }

  }

  /**
   * Inserts a new vertex to the storage and adds it to the cache if the cache ratio is not reached
   *
   * @param vetex the vertex that should be added to the store
   * @return true if the insertion was successful or false if the vertex was already contained.
   */
  def put(vertex: Vertex): Boolean = {
    if (inMemoryCache.contains(vertex.id) || persistentStore.get(vertex.id) != null) {
      false // Vertex already stored
    } else if (!cacheRatioReached) {
      inMemoryCache.put(vertex)
      var usedMemory = Runtime.getRuntime().totalMemory.asInstanceOf[Float] - Runtime.getRuntime().freeMemory
      if ((usedMemory / Runtime.getRuntime().maxMemory) > CACHING_THRESHOLD) {
        cacheRatioReached = true
        inMemoryCache.maxSize = inMemoryCache.size
      }
      cacheScores.put(vertex.id, 0.0)
      true
    } else {
      persistentStore.put(vertex)
      cacheScores.put(vertex.id, 0.0)
      true
    }
  }

  /**
   * Deletes a vertex from the store
   *
   * @param the id of the vertex to delete
   */
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

  /**
   * Retains the changed state of a vertex if it is not held in memory
   *
   * @param vertex the vertex that has a changed state that should be retained
   */
  def updateStateOfVertex(vertex: Vertex) {
    if (!inMemoryCache.contains(vertex.id)) {
      persistentStore.updateStateOfVertex(vertex)
    }
  }
  def size = cacheScores.size

  /**
   * applies a function to all vertices held by the storage
   */
  def foreach[U](f: (Vertex) => U) {
    inMemoryCache.foreach(f)
    persistentStore.foreach(vertex => {
    	if(!inMemoryCache.contains(vertex.id)) {
    	  f(vertex)
    	}
    })
  }

  def cleanUp {
    persistentStore.cleanUp
    inMemoryCache.cleanUp
  }

  /**
   * In-Memory cache implementation that evicts all vertices that are lower than the average of the whole store.
   */
  protected class InMemoryCache(storage: Storage) extends VertexStore {
    var maxSize = Long.MaxValue //gets reset if cache ratio is reached
    val cachedVertices = new ConcurrentHashMap[Any, Vertex](100000, 0.75f, 16) //TODO replace with non-concurrent data structure

    def contains(id: Any) = cachedVertices.containsKey(id)

    /**
     * Adds a vertex to the cache
     * and evicts the ones that are below average
     */
    def cache(vertex: Vertex) {
      if (!contains(vertex.id)) {
        if (isFull) {
          consolidateCache
        }
        if (!isFull) {
          cachedVertices.put(vertex.id, vertex)
        }
      }
    }

    def get(id: Any): Vertex = {
      cachedVertices.get(id)
    }

    def put(vertex: Vertex): Boolean = {
      if (!cachedVertices.containsKey(vertex.id)) {
        cachedVertices.put(vertex.id, vertex)
        true
      } else
        false
    }
    
    def remove(id: Any) = {
      cachedVertices.remove(id)
      storage.toCollect.remove(id)
      storage.toSignal.remove(id)
    }

    def updateStateOfVertex(vertex: Vertex) = {} // Not needed for in-memory implementation

    def foreach[U](f: (Vertex) => U) {
      val it = cachedVertices.values.iterator
      while (it.hasNext) {
        val vertex = it.next
        f(vertex)
      }
    }

    /**
     * removes all vertices from the cache that have a score below the average
     */
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

    def cleanUp = cachedVertices.clear

  }
}

/**
 * To allow mixing in the ScoredVertexCache with a storage configuration.
 * Uses a simple count metric that prioritizes vertices that are accessed most often.
 */
trait MostFrequentlyUsedCache extends DefaultStorage {
  def berkeleyDBFactory(storage: Storage) = new BerkeleyDBStorage(storage, ".")
  override protected def vertexStoreFactory = new ScoredVertexCache(berkeleyDBFactory, this, score => score + 1)
}

