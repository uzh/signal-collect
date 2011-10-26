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
import com.signalcollect.interfaces._
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.MessageBus
import collection.JavaConversions._
import scala.collection.mutable.HashMap

/**
 *  Vertex implementation that collects the most recent signals that have arrived on all edges.
 *  Users of the framework extend this class to implement a specific algorithm by defining a `collect` function.
 *
 *  @note The `collect` function receives as many signals as there are edges that have sent at least one signal already.
 *
 *  @param id unique vertex id.
 *  @param state the initial state of the vertex.
 *
 *  @author Philip Stutz
 *  @version 1.0.0
 *  @since 1.0.0
 */
abstract class DataGraphVertex[IdTypeParameter, StateTypeParameter](
  val id: IdTypeParameter,
  var state: StateTypeParameter)
  extends AbstractVertex with SumOfOutWeights with VertexGraphEditor {

  type Id = IdTypeParameter
  type State = StateTypeParameter

  /**
   *  The abstract `collect` function is algorithm specific and calculates the new vertex state.
   *
   *  @note If the edge along which a signal was sent is relevant, then mostRecentSignalMap can be used to access the edge id of a signal.
   *
   *  @param mostRecentSignals An iterable that returns the most recently received signal for each edge that has sent at least one signals already.
   *
   *  @return The new vertex state.
   *
   *  @note Beware of modifying and returning a referenced object,
   *  default signal scoring and termination detection fail in this case.
   */
  def collect(oldState: State, mostRecentSignals: Iterable[Signal]): State

  /**
   *  A map that has edge ids as keys and stores the most recent signal received along the edge with that id as the value for that key.
   */
  protected val mostRecentSignalMap = new HashMap[EdgeId[_, Id], Signal]()

  /**
   *  Utility function to filter out only certain signals of interest.
   */
  protected def signals[G](filterClass: Class[G]): Iterable[G] = {
    mostRecentSignalMap.values flatMap (value => Filter.bySuperClass(filterClass, value))
  }

  /**
   *  Returns the most recent signals received along edge id `id`.
   *
   *  @param id The edge id of the edge for which we would like to retrieve the most recent signal that was sent along it.
   */
  override def getMostRecentSignal(id: EdgeId[_, _]): Option[_] = {
    val signal = mostRecentSignalMap.get(id.asInstanceOf[EdgeId[_, Id]])
    signal match {
      case null => None
      case s => Some(s)
    }
  }

  /**
   *  Function that gets called by the framework whenever this vertex is supposed to collect new signals.
   *
   *  @param signals new signals that have arrived since the last time this vertex collected
   *
   *  @param messageBus an instance of MessageBus which can be used by this vertex to interact with the graph.
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any]) {
    val castS = signals.asInstanceOf[Iterable[SignalMessage[_, Id, Signal]]]
    // faster than scala foreach
    val i = castS.iterator
    while (i.hasNext) {
      val signalMessage = i.next
      mostRecentSignalMap.put(signalMessage.edgeId, signalMessage.signal)
    }
    state = collect(state, mostRecentSignalMap.values)
  }

  /**
   *  Returns ids of vertices that are known to be or have been predecessors at some point in time.
   */
  override def getVertexIdsOfPredecessors: Option[Iterable[_]] = {
    Some(mostRecentSignalMap.keys map (_.sourceId))
  }

}