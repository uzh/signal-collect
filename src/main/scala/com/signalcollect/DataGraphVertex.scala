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

import scala.Some.apply
import scala.collection.JavaConversions._

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
  def collect: State

  /**
   *  A map that has edge ids as keys and stores the most recent signal received along the edge with that id as the value for that key.
   *  
   *  Currently a Java HashMap is used as the implementation, but we will replace it with a more specialized
   *  implementation in a future release.
   */
  protected val mostRecentSignalMap: collection.mutable.Map[Any, Signal] = new java.util.HashMap[Any, Signal](0)

  def deliverSignal(signal: Any, sourceId: Option[Any]): Boolean = {
    assert(sourceId.isDefined, "Data graph vertices only make sense if the source id is known.")
    mostRecentSignalMap.put(sourceId.get, signal.asInstanceOf[Signal])
    false
  }

  def signals = mostRecentSignalMap.values

  /**
   *  Function that gets called by the framework whenever this vertex is supposed to collect new signals.
   *
   *  @param graphEditor an instance of GraphEditor which can be used by this vertex to interact with the graph.
   */
  override def executeCollectOperation(graphEditor: GraphEditor[Any, Any]) {
    super.executeCollectOperation(graphEditor)
    setState(collect)
  }

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect: Double = {
    if (!mostRecentSignalMap.isEmpty) {
      1.0
    } else if (edgesModifiedSinceCollectOperation) {
      1.0
    } else {
      0.0
    }
  }

}