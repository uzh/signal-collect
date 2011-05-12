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

import scala.collection.mutable.Set
import signalcollect.interfaces._
import util.collections.ConcurrentHashSet

/**
 * Stores a set of vertex IDs in main memory.
 */
class InMemoryVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var toHandle: Set[Any] = vertexSetFactory
  protected var toHandleSnapshot: Set[Any] = vertexSetFactory
  protected def vertexSetFactory = new ConcurrentHashSet[Any](100000, 0.75f, ComputeGraph.defaultNumberOfThreadsUsed)

  def +=(vertexId: Any): Unit = {
    toHandle.add(vertexId)
  }

  def -=(vertexId: Any): Unit = {
    toHandle.remove(vertexId)
  }

  def isEmpty: Boolean = {
    toHandle.isEmpty && toHandleSnapshot.isEmpty
  }

  def size: Long = { toHandle.size }

  def foreach[U](f: (Vertex[_, _]) => U) = {
    val i = toHandle.iterator
    while (i.hasNext) {
      f(vertexStore.vertices.get(i.next))
    }
    toHandle.clear
  }

  /**
   * Iterates through all stored IDs and applies a function to all vertices associated with these IDs.
   * The iteration process can also be interrupted via the breakConditionReached function parameter.
   * 
   * @param f							the function to apply to each vertex who's id is stored in the set
   * @param breakConditionReached		determines if the foreach-processing needs to be escaped do do other work
   * 									@see resumeProcessingSnapshot on how to resume an aborted processing.
   */
  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U, breakConditionReached: () => Boolean): Boolean = {
    var processedAll = false
    toHandleSnapshot = toHandle
    toHandle = vertexSetFactory
    val i = toHandleSnapshot.iterator
    while (i.hasNext && !breakConditionReached()) {
      val currentVertexId = i.next
      f(vertexStore.vertices.get(currentVertexId))
      toHandleSnapshot.remove(currentVertexId)
    }
    if (toHandleSnapshot.isEmpty) {
      processedAll = true
    }
    processedAll
  }

  /**
   * Resumes an aborted iteration process
   * Can be interrupted again.
   */
  def resumeProcessingSnapshot[U](f: (Vertex[_, _]) => U, breakConditionReached: () => Boolean): Boolean = {
    var processedAll = false
    val i = toHandleSnapshot.iterator

    while (i.hasNext && !breakConditionReached()) {
      val currentVertexId = i.next
      f(vertexStore.vertices.get(currentVertexId))
      toHandleSnapshot.remove(currentVertexId)
    }
    if (toHandleSnapshot.isEmpty) {
      processedAll = true
    }
    processedAll
  }
}