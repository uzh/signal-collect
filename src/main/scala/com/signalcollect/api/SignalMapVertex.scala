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

package com.signalcollect.api

import com.signalcollect.implementations.graph._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import com.signalcollect.interfaces._
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.MessageBus

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
abstract class SignalMapVertex[IdTypeParameter, StateTypeParameter](
  val id: IdTypeParameter,
  var state: StateTypeParameter)
  extends AbstractVertex {

  type Id = IdTypeParameter
  type State = StateTypeParameter
  
  protected val mostRecentSignalMap: Map[EdgeId[Id, _], Signal] = HashMap[EdgeId[Id, _], Signal]() // key: signal source id, value: signal

  protected def signals[G](filterClass: Class[G]): Iterable[G] = {
    mostRecentSignalMap.values flatMap (value => Filter.bySuperClass(filterClass, value))
  }

  def getMostRecentSignal(id: EdgeId[_, _]): Option[_] = {
    mostRecentSignalMap.get(id.asInstanceOf[EdgeId[Id, _]])
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any]) {
    val castS = signals.asInstanceOf[Iterable[SignalMessage[Id, Any, Signal]]]
    castS foreach { signal =>
      mostRecentSignalMap.put(signal.edgeId, signal.signal)
    }
    state = collect(mostRecentSignalMap.values.asInstanceOf[Iterable[Signal]])
  }

  def getVertexIdsOfPredecessors: Option[Iterable[_]] = {
    Some(mostRecentSignalMap.keys map (_.sourceId))
  }
  
}