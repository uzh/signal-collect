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
import java.util.HashSet

/**
 * Stores a set of vertex IDs in main memory.
 */
class InMemoryVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var toHandle: HashSet[Any] = vertexSetFactory
  protected var toHandleSnapshot: HashSet[Any] = vertexSetFactory
  protected var verticesDoneInSnapshot = 0
  protected def vertexSetFactory = new HashSet[Any]()
  protected var snapshotIterator = toHandleSnapshot.iterator

  def add(vertexId: Any): Unit = {
    toHandle.add(vertexId)
  }

  def remove(vertexId: Any): Unit = {
    toHandle.remove(vertexId)
  }

  def clear {
    toHandle = vertexSetFactory
  }

  def isEmpty: Boolean = toHandle.isEmpty && !snapshotIterator.hasNext

  def size: Long = toHandle.size + toHandleSnapshot.size - verticesDoneInSnapshot

  def foreach[U](f: (Vertex[_, _]) => U) = {
    val i = toHandle.iterator
    while (i.hasNext) {
      val vertex = vertexStore.vertices.get(i.next)
      f(vertex)
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
    
    if (!snapshotIterator.hasNext) {
    	verticesDoneInSnapshot = 0
    	toHandleSnapshot = toHandle
    	toHandle = vertexSetFactory
    	snapshotIterator = toHandleSnapshot.iterator
    }
    
    while (snapshotIterator.hasNext && !breakConditionReached()) {
    	val currentVertexId = snapshotIterator.next
    	f(vertexStore.vertices.get(currentVertexId))
    	verticesDoneInSnapshot += 1
    }
    !snapshotIterator.hasNext


  }
  
  def cleanUp = { 
    toHandle.clear
    toHandleSnapshot.clear
  }
}