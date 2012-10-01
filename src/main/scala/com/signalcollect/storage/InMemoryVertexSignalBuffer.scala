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

import com.signalcollect.interfaces.{ VertexSignalBuffer, SignalMessage, Storage }
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.IndexedSeq

/**
 * Stores Signals that were received by the worker but not collected by the vertices yet
 */
class InMemoryVertexSignalBuffer extends VertexSignalBuffer {

  val undeliveredSignals = new ConcurrentHashMap[Any, ArrayBuffer[SignalMessage[_]]](16, 0.75f, 1) //key: recipients id, value: signals for that recipient
  var iterator = undeliveredSignals.keySet.iterator

  /**
   * Adds a new signal for a specific recipient to the buffer
   * If there are already signals for that recipient the new signal is added to the ones waiting otherwise a new map entry is created
   *
   * Notice: Signals are not checked for valid id when inserted to the buffer.
   *
   * @param signalMessage the signal message that should be buffered before collection
   */
  def addSignal(signalMessage: SignalMessage[_]) {
    if (undeliveredSignals.containsKey(signalMessage.edgeId.targetId)) {
      undeliveredSignals.get(signalMessage.edgeId.targetId).add(signalMessage)
    } else {
      val signalsForVertex = new ArrayBuffer[SignalMessage[_]]()
      signalsForVertex.add(signalMessage)
      undeliveredSignals.put(signalMessage.edgeId.targetId, signalsForVertex)
    }
  }

  /**
   * If the map contains no entry for that id a new entry is created with no signals buffered
   * This can be useful when a vertex still needs to collect even though no new signals are available
   *
   * @ vertexId the ID of a vertex that should collect regardless of the existence of signals for it
   */
  def addVertex(vertexId: Any) {
    if (!undeliveredSignals.containsKey(vertexId)) {
      undeliveredSignals.put(vertexId, new ArrayBuffer[SignalMessage[_]]())
    }
  }

  /**
   * Manually removes the vertexId and its associated signals from the map
   * Should only be used when a vertex is removed from the map to remove the vertex after successfully collecting use the parameter in the foreach function
   *
   * @param vertexId the ID of the vertex that needs to be removed from the map
   */
  def remove(vertexId: Any) {
    undeliveredSignals.remove(vertexId)
  }

  def isEmpty: Boolean = undeliveredSignals.isEmpty

  /**
   * Returns the number of vertices for which the buffer has signals stored.
   */
  def size = undeliveredSignals.size

  /**
   * Iterates through all signals in the buffer and applies the specified function to each entry
   * Allows the the loop to be escaped and to resume work at the same position
   *
   * @param f 				the function to apply to each entry in the map
   * @param clearWhenDone	determines if the map should be cleared when all entries are processed
   * @param breakCondition 	determines if the loop should be escaped before it is done
   * @return 				has the execution handled all elements in the list i.e. has it not been interrupted by the break condition
   */
  def foreach[U](f: (Any, IndexedSeq[SignalMessage[_]]) => U, removeAfterProcessing: Boolean, breakCondition: () => Boolean = () => false): Boolean = {

    if (!iterator.hasNext) {
      iterator = undeliveredSignals.keySet.iterator
    }

    while (iterator.hasNext && !breakCondition()) {
      val currentId = iterator.next
      f(currentId, undeliveredSignals.get(currentId))
      if (removeAfterProcessing) {
        remove(currentId)
      }
    }
    !iterator.hasNext
  }

  def cleanUp = undeliveredSignals.clear
}