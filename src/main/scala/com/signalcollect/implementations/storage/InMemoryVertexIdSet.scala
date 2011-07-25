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

import scala.collection.mutable.Set
import com.signalcollect.interfaces._
import java.util.HashSet

/**
 * Stores a set of vertex IDs in main memory.
 */
class InMemoryVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var toHandle: HashSet[Any] = vertexSetFactory //Stores all the IDs of the vertices that need to be processed
  protected def vertexSetFactory = new HashSet[Any]()

  def add(vertexId: Any): Unit = {
    toHandle.add(vertexId)
  }

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
    val i = toHandle.iterator
    while (i.hasNext) {
      f(i.next)
    }
    if (removeAfterProcessing) {
      toHandle.clear
    }
  }

  def cleanUp = {
    toHandle.clear
  }
}