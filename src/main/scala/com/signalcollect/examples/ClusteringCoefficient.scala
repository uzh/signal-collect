/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

class ClusteringCoefficientVertex(id: Any) extends DataGraphVertex(id, 0.0) {

  type Signal = Set[Any]

  lazy val maxEdges = outgoingEdges.size * (outgoingEdges.size - 1)
  lazy val neighbourIds = getTargetIdsOfOutgoingEdges.toSet

  def collect = {
    if (maxEdges != 0) {
      val edgesBetweenNeighbours = signals.map(neighbourIds.intersect(_)).map(_.size).sum
      edgesBetweenNeighbours / maxEdges.toDouble
    } else {
      Double.NaN
    }
  }

}

class ClusteringCoefficientEdge(t: Any) extends DefaultEdge(t) {
  type Source = ClusteringCoefficientVertex

  def signal = source.neighbourIds

}

object ClusteringCoefficient extends App {
  val graph = GraphBuilder.build
  graph.addVertex(new ClusteringCoefficientVertex(1))
  graph.addVertex(new ClusteringCoefficientVertex(2))
  graph.addVertex(new ClusteringCoefficientVertex(3))
  graph.addVertex(new ClusteringCoefficientVertex(4))
  graph.addVertex(new ClusteringCoefficientVertex(5))
  graph.addVertex(new ClusteringCoefficientVertex(6))
  graph.addEdge(1, new ClusteringCoefficientEdge(2))
  graph.addEdge(2, new ClusteringCoefficientEdge(1))
  graph.addEdge(1, new ClusteringCoefficientEdge(3))
  graph.addEdge(3, new ClusteringCoefficientEdge(1))
  graph.addEdge(1, new ClusteringCoefficientEdge(4))
  graph.addEdge(4, new ClusteringCoefficientEdge(1))
  graph.addEdge(1, new ClusteringCoefficientEdge(5))
  graph.addEdge(5, new ClusteringCoefficientEdge(1))
  graph.addEdge(2, new ClusteringCoefficientEdge(3))
  graph.addEdge(3, new ClusteringCoefficientEdge(2))
  graph.addEdge(3, new ClusteringCoefficientEdge(5))
  graph.addEdge(5, new ClusteringCoefficientEdge(3))
  graph.addEdge(6, new ClusteringCoefficientEdge(5))
  graph.addEdge(5, new ClusteringCoefficientEdge(6))
  graph.addEdge(6, new ClusteringCoefficientEdge(1))
  graph.addEdge(1, new ClusteringCoefficientEdge(6))

  val stats = graph.execute
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
