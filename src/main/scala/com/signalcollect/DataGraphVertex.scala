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

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.util.collections.Filter
import scala.collection.mutable.IndexedSeq

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
 */
abstract class DataGraphVertex[Id, State](
    val id: Id,
    var state: State) extends AbstractVertex[Id, State] with SumOfOutWeights[Id, State] {

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
   *  @note If the edge along which a signal was sent is relevant, then mostRecentSignalMap can be used to access the edge id of a signal.
   *
   *  @param mostRecentSignals An iterable that returns the most recently received signal for each edge that has sent at least one signal already.
   *
   *  @return The new vertex state.
   *
   *  @note Beware of modifying and returning a referenced object,
   *  default signal scoring and termination detection fail in this case.
   */
  def collect(oldState: State, mostRecentSignals: Iterable[Signal], graphEditor: GraphEditor): State

  /**
   *  A map that has edge ids as keys and stores the most recent signal received along the edge with that id as the value for that key.
   */
  protected val mostRecentSignalMap = new HashMap[Any, Signal]()

  /**
   *  Utility function to filter out only certain signals of interest.
   */
  protected def signals[G](filterClass: Class[G]): Iterable[G] = {
    mostRecentSignalMap.values flatMap (value => Filter.bySuperClass(filterClass, value))
  }

  /**
   *  Returns the most recent signal received from the vertex with id `id`.
   *
   *  @param id The id of the vertex from which we received a signal.
   */
  def getMostRecentSignal(id: Any): Option[_] =
    mostRecentSignalMap.get(id) match {
      case null => None
      case s    => Some(s)
    }

  /**
   *  Function that gets called by the framework whenever this vertex is supposed to collect new signals.
   *
   *  @param signals new signals that have arrived since the last time this vertex collected
   *
   *  @param messageBus an instance of MessageBus which can be used by this vertex to interact with the graph.
   */
  override def executeCollectOperation(signals: IndexedSeq[SignalMessage[_]], graphEditor: GraphEditor) {
    super.executeCollectOperation(signals, graphEditor)
    val castS = signals.asInstanceOf[Iterable[SignalMessage[Signal]]]
    // Faster than Scala foreach.
    val i = castS.iterator
    while (i.hasNext) {
      val signalMessage = i.next
      mostRecentSignalMap.put(signalMessage.edgeId.sourceId, signalMessage.signal)
    }
    state = collect(state, mostRecentSignalMap.values, graphEditor)
  }

}