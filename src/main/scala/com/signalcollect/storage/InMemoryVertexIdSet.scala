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

package com.signalcollect.storage

import com.signalcollect.interfaces._
import scala.collection.mutable.LinkedHashSet
import com.signalcollect.Vertex

/**
 * Stores a set of vertex IDs in main memory and allows iterating through them via a custom foreach function for processing the entries.
 */
class InMemoryVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var toHandle: LinkedHashSet[Any] = new LinkedHashSet[Any]() //Stores all the IDs of the vertices that need to be processed

  /**
   * Adds a new ID to the collection
   *
   * @param vetexId the ID of the vertex that should be added to the collection.
   */
  def add(vertexId: Any): Unit = {
    toHandle.add(vertexId)
  }

  /**
   * Removes an ID from the collection
   *
   * @param vertexId the ID of the vertex that should be removed
   */
  def remove(vertexId: Any): Unit = {
    toHandle.remove(vertexId)
  }

  def isEmpty: Boolean = toHandle.isEmpty

  def size: Int = toHandle.size

  /**
   * Applies the specified function to each vertex id and removes the ids if necessary
   *
   * @param f the function to apply to each id
   * @removeAfterProcessing whether the ids should be deleted after they are covered by the function
   */
  def foreach[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    if (!isEmpty) {
      val i = toHandle.iterator
      while (i.hasNext) {
        f(i.next)
      }
      if (removeAfterProcessing) {
        cleanUp
      }
    }
  }

  def applyToNext[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    if (!isEmpty) {
      val vertexId = toHandle.head
      f(vertexId)
      if (removeAfterProcessing) {
        toHandle.remove(vertexId)
      }
    }
  }

  /**
   * Ignored for this implementation.
   */
  def updateStateOfVertex(vertex: Vertex[_, _]) = {}

  /**
   * Removes all entries from the collection.
   */
  def cleanUp = {
    toHandle = null
    toHandle = new LinkedHashSet[Any]()
  }
}