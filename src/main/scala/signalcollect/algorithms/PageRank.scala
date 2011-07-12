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

package signalcollect.algorithms

import signalcollect.api._
import signalcollect.implementations.graph.SumOfOutWeights
import signalcollect.configuration._
import java.io.{ByteArrayOutputStream, ByteArrayInputStream, ObjectOutputStream, ObjectInputStream}
import signalcollect.interfaces.{Edge, Signal}
import java.util.LinkedList

/**
 * Represents an edge in a PageRank compute graph
 *
 *  @param s: the identifier of the source vertex
 *  @param t: the identifier of the target vertex
 */
class Link(s: Any, t: Any) extends DefaultEdge(s, t) {

  type SourceVertexType = Page

  /**
   * The signal function calculates how much rank the source vertex
   *  transfers to the target vertex.
   */
  def signal(sourceVertex: Page) = sourceVertex.state * weight / sourceVertex.sumOfOutWeights

}

/**
 * Represents a page in a PageRank compute graph
 *
 *  @param id: the identifier of this vertex
 *  @param dampingFactor: @see <a href="http://en.wikipedia.org/wiki/PageRank">PageRank algorithm</a>
 */
class Page(id: Any, dampingFactor: Double = 0.85) extends SignalMapVertex(id, 1 - dampingFactor) with SumOfOutWeights[Any, Double] {

  type UpperSignalTypeBound = Double
	
  /**
   * The collect function calculates the rank of this vertex based on the rank
   *  received from neighbors and the damping factor.
   */
  def collect: Double = 1 - dampingFactor + dampingFactor * mostRecentSignals.foldLeft(0.0)(_ + _)

  override def scoreSignal: Double = {
    lastSignalState match {
      case None => 1
      case Some(oldState) => (state - oldState).abs
    }
  }
}

/** Builds a PageRank compute graph and executes the computation */
object PageRank extends App {
  val cg = new ComputeGraphBuilder().build
  cg.addVertex(new Page(1))
  cg.addVertex(new Page(2))
  cg.addVertex(new Page(3))
  cg.addEdge(new Link(1, 2))
  cg.addEdge(new Link(2, 1))
  cg.addEdge(new Link(2, 3))
  cg.addEdge(new Link(3, 2))
  val stats = cg.execute(new ExecutionConfiguration(executionMode=SynchronousExecutionMode))
  println(stats)
  cg.foreachVertex (println(_))
  cg.shutdown
}
