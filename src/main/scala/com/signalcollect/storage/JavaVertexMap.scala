/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
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

package com.signalcollect.storage

import java.util.HashMap

import scala.collection.JavaConversions.asScalaIterator

import com.signalcollect.Vertex
import com.signalcollect.interfaces.VertexStore

/**
 * Stores all vertices in a in-memory HashMap data structure.
 */
class JavaVertexMap[@specialized(Int, Long) Id] extends VertexStore[Id] {
  protected var vertexMap = new HashMap[Id, Vertex[Id, _]]()

  /**
   * Returns a vertex from the store that has the specified id.
   *
   * @param id the ID of the vertex to retrieve
   * @return the vertex object or null if the vertex is not contained in the store
   */
  def get(id: Id): Vertex[Id, _] = {
    vertexMap.get(id)
  }

  /**
   * Inserts a vertex in the collection if the vertex collection does not already contain a vertex with the same id.
   *
   * @param the vertex to insert
   * @return true if the insertion was successful, false if the storage already contained a vertex with the same id.
   */
  def put(vertex: Vertex[Id, _]): Boolean = {
    if (!vertexMap.containsKey(vertex.id)) {
      vertexMap.put(vertex.id, vertex)
      true
    } else
      false
  }

  /**
   * Removes a vertex from the collection and also removes its entires in the toSignal and toCollect collections.
   *
   * @param id the ID of the vertex to remove
   */
  def remove(id: Id) = {
    val vertex = vertexMap.remove(id)
  }

  def isEmpty = vertexMap.isEmpty

  def size = vertexMap.size

  def stream: Stream[Vertex[Id, _]] = vertexMap.values.iterator.toStream

  /**
   * Applies the supplied function to each stored vertex
   *
   * @param f Function to apply to each stored vertex
   */
  def foreach(f: Vertex[Id, _] => Unit) = {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex = it.next
      f(vertex)
    }
  }

  def process(p: Vertex[Id, _] => Unit, numberOfVertices: Option[Int] = None): Int = {
    val it = vertexMap.values.iterator
    val limit: Int = math.min(vertexMap.size, numberOfVertices.getOrElse(vertexMap.size))
    var removed = 0
    while (it.hasNext && removed < limit) {
      val vertex = it.next
      p(vertex)
      it.remove
      removed += 1
    }
    removed
  }

  def processWithCondition(p: Vertex[Id, _] => Unit, breakCondition: () => Boolean): Int = {
    val it = vertexMap.values.iterator
    var removed = 0
    while (it.hasNext && !breakCondition()) {
      val vertex = it.next
      p(vertex)
      it.remove
      removed += 1
    }
    removed
  }

}