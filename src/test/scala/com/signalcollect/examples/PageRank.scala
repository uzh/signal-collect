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

package com.signalcollect.examples

import com.signalcollect.interfaces.MessageBus
import com.signalcollect.factory.storage._
import com.signalcollect._

/**
 * Represents an edge in a PageRank compute graph
 *
 *  @param s: the identifier of the source vertex
 *  @param t: the identifier of the target vertex
 */
class PageRankEdge(s: Any, t: Any) extends DefaultEdge(s, t) {
  
  type SourceVertex = PageRankVertex
  
  /**
   * The signal function calculates how much rank the source vertex
   *  transfers to the target vertex.
   */
  override def signal(sourceVertex: PageRankVertex) = sourceVertex.state * weight / sourceVertex.sumOfOutWeights

}

/**
 * Represents a page in a PageRank compute graph
 *
 *  @param id: the identifier of this vertex
 *  @param dampingFactor: @see <a href="http://en.wikipedia.org/wiki/PageRank">PageRank algorithm</a>
 */
class PageRankVertex(id: Any, dampingFactor: Double = 0.85) extends DataGraphVertex(id, 1 - dampingFactor) {

  type Signal = Double

  /**
   * The collect function calculates the rank of this vertex based on the rank
   *  received from neighbors and the damping factor.
   */
  def collect(oldState: State, mostRecentSignals: Iterable[Double]): Double = {
    1 - dampingFactor + dampingFactor * mostRecentSignals.sum
  }

  override def scoreSignal: Double = {
    lastSignalState match {
      case None => 1
      case Some(oldState) => (state - oldState).abs
    }
  }
  
}

/** Builds a PageRank compute graph and executes the computation */
object PageRank extends App {
  val graph = GraphBuilder.build
  graph.addVertex(new PageRankVertex(1))
  graph.addVertex(new PageRankVertex(2))
  graph.addVertex(new PageRankVertex(3))
  graph.addEdge(new PageRankEdge(1, 2))
  graph.addEdge(new PageRankEdge(2, 1))
  graph.addEdge(new PageRankEdge(2, 3))
  graph.addEdge(new PageRankEdge(3, 2))
  val stats = graph.execute
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
