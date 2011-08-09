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

import java.util.Set
import com.signalcollect.interfaces._
import java.util.HashMap

/**
 * Stores all vertices in a in-memory HashMap data structure.
 */
class InMemoryStorage(storage: Storage) extends VertexStore {
  protected var vertexMap = new HashMap[Any, Vertex]()

  /**
   * Returns a vertex from the store that has the specified id.
   *
   * @param id the ID of the vertex to retrieve
   * @return the vertex object or null if the vertex is not contained in the store
   */
  def get(id: Any): Vertex = {
    vertexMap.get(id)
  }

  /**
   * Inserts a vertex in the collection if the vertex collection does not already contain a vertex with the same id.
   * 
   * @param the vertex to insert
   * @return true if the insertion was successful, false if the storage already contained a vertex with the same id.
   */
  def put(vertex: Vertex): Boolean = {
    if (!vertexMap.containsKey(vertex.id)) {
      vertexMap.put(vertex.id, vertex)
      storage.toCollect.addVertex(vertex.id)
      storage.toSignal.add(vertex.id)
      true
    } else
      false
  }
  
  /**
   * Removes a vertex from the collection and also removes its entires in the toSignal and toCollect collections.
   * 
   * @param id the ID of the vertex to remove
   */
  def remove(id: Any) = {
    vertexMap.remove(id)
    storage.toCollect.remove(id)
    storage.toSignal.remove(id)
  }

  /**
   * Is not needed for this implementation because the state does not need to be retained, since the objects are passed by reference and changes
   * in the vertex's state are reflected immediately.
   * 
   * @param vertex the vertex that would have to be updated
   */
  def updateStateOfVertex(vertex: Vertex) = {}

  def foreach[U](f: Vertex => U) {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex = it.next
      f(vertex)
    }
  }

  /**
   * Number of vertices held by the collection
   * 
   * @return number of vertices
   */
  def size: Long = vertexMap.size

  /**
   * Removes all the vertices from the collection.
   */
  def cleanUp = vertexMap.clear
}