/*
 *  @author Philip Stutz
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

package signalcollect.api

import signalcollect.interfaces._
import signalcollect.implementations.graph.AbstractEdge

/**
 * [[signalcollect.interfaces.Edge]] implementation that only signals
 * when the signal has changed compared to the last signal sent.
 *
 * @param sourceId id of this edge's source vertex
 * @param targetId id of this edges's target vertex
 *
 * See [[signalcollect.api.DefaultEdge]] for more information about edges
 * in general.
 */
abstract class OnlySignalOnChangeEdge[SourceIdType, TargetIdType](
  val sourceId: SourceIdType,
  val targetId: TargetIdType)
  extends AbstractEdge[SourceIdType, TargetIdType] {

  /** Last signal sent along this edge */
  var lastSignalSent: Option[SignalType] = None

  /**
   * Calculates the new signal, compares it with the last signal sent and
   * only sends a signal if they are not equal.
   */
  override def executeSignalOperation(mb: MessageBus[Any, Any]) {
    val newSignal = signal
    if (!lastSignalSent.isDefined || !lastSignalSent.get.equals(newSignal)) {
      mb.sendToWorkerForIdHash(Signal(sourceId, targetId, newSignal), targetHashCode)
      lastSignalSent = Some(newSignal)
    }
  }
}