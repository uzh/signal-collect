/*
 *  @author Daniel Strebel
 *  
 *  Copyright 2010 University of Zurich
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
 *  
 */

package signalcollect.implementations.worker

import signalcollect.interfaces._
import java.util.{ Map, HashMap }

/**
 * Buffers signals for vertices right at the worker and delivers them with the collect invocation.
 * This helps with the on disk approach to reduce the necessary number of serialzation/deserialization steps.
 */
trait SignalBuffer extends AbstractWorker {

  protected val undeliveredSignals: Map[Any, List[Signal[_, _, _]]] = new HashMap[Any, List[Signal[_, _, _]]]() // key: signal target id, value: List of signals for a vertex

  override protected def isConverged = super.isConverged && undeliveredSignals.isEmpty

  protected def bufferSignal(signal: Signal[_, _, _]) {
    if (undeliveredSignals.containsKey(signal.targetId)) {
      undeliveredSignals.put(signal.targetId, undeliveredSignals.get(signal.targetId) ++ List(signal))
    } else {
      val signalsForVertex = List(signal)
      undeliveredSignals.put(signal.targetId, signalsForVertex)
    }
    vertexStore.toCollect.add(signal.targetId)
  }

  override protected def processSignal(signal: Signal[_, _, _]) {
    if (signal.targetId == ALL) {
      vertexStore.vertices foreach (deliverSignal(signal, _))
    } else {
      bufferSignal(signal)
    }
  }

  override protected def collect(vertex: Vertex[_, _]): Boolean = {

    if (vertex.scoreCollect > collectThreshold || undeliveredSignals.containsKey(vertex.id)) {
      counters.collectOperationsExecuted += 1
      vertex.executeCollectOperation(Some(undeliveredSignals.get(vertex.id)))
      undeliveredSignals.remove(vertex.id)
      vertexStore.vertices.updateStateOfVertex(vertex)
      true
    } else {
      false
    }
  }
}