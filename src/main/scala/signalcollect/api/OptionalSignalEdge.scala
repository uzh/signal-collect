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
 * [[signalcollect.interfaces.Edge]] implementation that allows the
 * signal function to return an instance of [[scala.Option]]. If
 * [[scala.None]] is returned, no signal is sent.
 *
 * @param sourceId id of this edge's source vertex
 * @param targetId id of this edges's target vertex
 *
 * See [[signalcollect.api.DefaultEdge]] for more information about edges
 * in general.
 */
abstract class OptionalSignalEdge[SourceIdType, TargetIdType](
  val sourceId: SourceIdType,
  val targetId: TargetIdType)
  extends AbstractEdge[SourceIdType, TargetIdType] {

  /**
   * More specific signal function that returns a [[scala.Option]] 
   */
  def signal: Option[_]

  /**
   * Calculates the new signal. If the [[scala.Option]] is defined,
   * then the value is sent. Else nothing is sent.
   */
  override def executeSignalOperation(mb: MessageBus[Any, Any]) {
    val optionalSignal = signal
    if (optionalSignal.isDefined) {
      mb.sendToWorkerForVertexIdHash(Signal(sourceId, targetId, optionalSignal.get), targetHashCode)
    }
  }
}