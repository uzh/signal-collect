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

package com.signalcollect

import com.signalcollect.implementations.graph._
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.SignalMessage
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer
import com.signalcollect.interfaces._

/**
 * [[com.signalcollect.interfaces.Vertex]] implementation that offers only
 * a subset of the [[com.signalcollect.api.DefaultVertex]] functionality
 * to save memory.
 *
 * @param id unique vertex id
 * @param initialState initial state of this vertex
 *
 * See [[com.signalcollect.api.DefaultVertex]] for more information about vertices
 * in general.
 */
abstract class DataFlowVertex[IdTypeParameter, StateTypeParameter](
  val id: IdTypeParameter,
  var state: StateTypeParameter)
  extends AbstractVertex with ResetStateAfterSignaling with VertexGraphEditor {

  type Id = IdTypeParameter
  type State = StateTypeParameter

  /**
   * The abstract "collect" function is algorithm specific and has to be implemented by a user of the API
   * this function will be called during algorithm execution. It is meant to calculate a new vertex state
   * based on the {@link Signal}s received by this vertex.
   */
  def collect(uncollectedSignals: Iterable[Signal]): State
  
  /** a buffer containing uncollected messages */
  protected var uncollectedMessages: Iterable[SignalMessage[_, _, Signal]] = _

  /** traversable uncollected signals */
  protected def uncollectedSignals[G](filterClass: Class[G]): Iterable[G] = {
    uncollectedMessages flatMap (message => Filter.bySuperClass(filterClass, message.signal))
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any]) {
    uncollectedMessages = signals.asInstanceOf[Iterable[SignalMessage[_, _, Signal]]]
    state = collect((uncollectedMessages map (_.signal)).asInstanceOf[Iterable[Signal]])
  }

}
