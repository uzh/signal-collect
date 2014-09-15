/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.factory.messagebus.IntIdDoubleSignalMessageBusFactory

/**
 * Placeholder edge that gets discarded by memory efficient vertices that
 * have their own internal edge representations.
 */
class PlaceholderEdge[Id](targetId: Id) extends DefaultEdge(targetId) {
  def signal = ???
}

class EfficientPageRankVertex(id: Int)
  extends MemoryEfficientDataFlowVertex[Double, Double](id = id, state = 0.15) {

  type OutgoingSignalType = Double

  def computeSignal(edgeId: Int) = ???

  override def executeSignalOperation(graphEditor: GraphEditor[Int, Double]) {
    if (edgeCount != 0) {
      val signal = (state - lastSignalState) / edgeCount
      targetIds.foreach(graphEditor.sendSignal(signal, _))
    }
    lastSignalState = state
  }

  def collect(signal: Double): Double = {
    state + 0.85 * signal
  }

  def scoreSignal = {
    state - lastSignalState
  }

  def addTargetId(targetId: Int) {
    _targetIds.insert(targetId)
  }

}

/** Builds a PageRank compute graph and executes the computation */
object MemoryEfficientPageRank extends App {
  val graph = new GraphBuilder[Int, Double]().
    withMessageBusFactory(new IntIdDoubleSignalMessageBusFactory(10000)).
    //    withConsole(true).
    build
  graph.addVertex(new EfficientPageRankVertex(1))
  graph.addVertex(new EfficientPageRankVertex(2))
  graph.addVertex(new EfficientPageRankVertex(3))
  graph.addEdge(1, new PlaceholderEdge(2))
  graph.addEdge(2, new PlaceholderEdge(1))
  graph.addEdge(2, new PlaceholderEdge(3))
  graph.addEdge(3, new PlaceholderEdge(2))
  graph.awaitIdle
  val stats = graph.execute //(ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive))
  println(stats)

  graph.foreachVertex(println(_))
  graph.shutdown
}
