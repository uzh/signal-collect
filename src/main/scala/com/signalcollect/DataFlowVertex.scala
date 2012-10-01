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

import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.util.collections.Filter
import scala.collection.mutable.IndexedSeq

/**
 *  Vertex implementation that collects all the signals that have arrived since the last
 *  time this vertex has collected. Users of the framework extend this class to implement
 *  a specific algorithm by defining a `collect` function.
 *
 *  @note The `collect` function receives all signals that arrived at this vertex but have not
 *  been collected yet as a parameter.
 *
 *  @param id Unique vertex id.
 *  @param state The initial state of the vertex.
 *  @param resetState The state will be set to `resetState` after signaling.
 *
 *  @author Philip Stutz
 */
abstract class DataFlowVertex[Id, State](
  val id: Id,
  var state: State,
  val resetState: State)
  extends AbstractVertex[Id, State] with ResetStateAfterSignaling[Id, State] {
  
  type Signal

  /**
   *  @return the object that stores the current state for this `Vertex`.
   */
  def getState: State = state
  def setState(s: State) {
    state = s
  }
  
  /**
   *  The abstract `collect` function is algorithm specific and calculates the new vertex state.
   *
   *  @param uncollectedSignals all signals received by this vertex since the last time this function was executed
   *
   *  @return The new vertex state.
   *
   *  @note Beware of modifying and returning a referenced object,
   *  default signal scoring and termination detection fail in this case.
   */
  def collect(oldState: State, uncollectedSignals: Iterable[Signal], graphEditor: GraphEditor): State

  /**
   *  Function that gets called by the framework whenever this vertex is supposed to collect new signals.
   *
   *  @param signals new signals that have arrived since the last time this vertex collected
   *
   *  @param messageBus an instance of MessageBus which can be used by this vertex to interact with the graph.
   */
  override def executeCollectOperation(signals: IndexedSeq[SignalMessage[_]], graphEditor: GraphEditor) {
    super.executeCollectOperation(signals, graphEditor)
    state = collect(state, (signals map (_.signal)).asInstanceOf[Iterable[Signal]], graphEditor)
  }

}
