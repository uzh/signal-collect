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

import com.signalcollect.interfaces.{VertexSignalBuffer, Signal, Storage}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.ArrayBuffer

class DefaultVertexSignalBuffer extends VertexSignalBuffer {
  
  val undeliveredSignals = new ConcurrentHashMap[Any, ArrayBuffer[Signal[_, _, _]]]()
  var iterator = undeliveredSignals.keySet.iterator
  
 def addSignal(signal: Signal[_, _, _]) {
    if (undeliveredSignals.containsKey(signal.targetId)) {
      undeliveredSignals.get(signal.targetId).append(signal)
    } else {
      val signalsForVertex = ArrayBuffer[Signal[_, _, _]](signal)
      undeliveredSignals.put(signal.targetId, signalsForVertex)
    }
  }
  
  def addVertex(vertexId: Any) {
    if (!undeliveredSignals.containsKey(vertexId)) {
      undeliveredSignals.put(vertexId, ArrayBuffer[Signal[_, _, _]]())
    }
  }
  
 def remove(vertexId: Any) {
   undeliveredSignals.remove(vertexId)
 }
 
 def clear = undeliveredSignals.clear
 
 def isEmpty: Boolean = undeliveredSignals.isEmpty
 
 def size=undeliveredSignals.size
 
 def foreach[U](f: (Any, Iterable[Signal[_, _, _]]) => U) {
   iterator = undeliveredSignals.keySet.iterator
   while(iterator.hasNext) {
     val currentId = iterator.next
     f(currentId, undeliveredSignals.get(currentId))
   }
 }
 
 def foreachWithSnapshot[U](f: (Any, Iterable[Signal[_, _, _]]) => U, breakConditionReached: () => Boolean): Boolean = {
   if(!iterator.hasNext) {
     iterator = undeliveredSignals.keySet.iterator
   }
   while(iterator.hasNext && !breakConditionReached()) {
     val currentId = iterator.next
     f(currentId, undeliveredSignals.get(currentId))
   }
   !iterator.hasNext
 }
 
 def cleanUp = clear
}