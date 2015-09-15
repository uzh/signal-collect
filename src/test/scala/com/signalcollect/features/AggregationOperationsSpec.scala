/*
 *  @author Philip Stutz
 *
 *  Copyright 2011 University of Zurich
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

package com.signalcollect.features

import akka.actor.ActorSystem
import com.signalcollect._
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.SudokuCell
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.signalcollect.util.TestAnnouncements

class AggregationOperationsSpec extends FlatSpec with Matchers with TestAnnouncements {

  "SumOfStates" should "sum all states correctly" in {
    val system = TestConfig.actorSystem()
    def createGraph = {
      val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(TestConfig.prefix).build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new SudokuCell(1, None))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph
    }
    val graph = createGraph
    try {
      graph.execute(ExecutionConfiguration.withSignalThreshold(0))
      val sumOfStates = graph.aggregate(new SumOfStates[Double]).getOrElse(0.0)
      math.abs(sumOfStates - 2.0) <= 0.0001
    } finally {
      graph.shutdown
    }
  }

  "ProductOfStates" should "multiply all states correctly" in {
    val system = TestConfig.actorSystem()
    def createGraph = {
      val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(system.name).build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new SudokuCell(1, None))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph
    }
    val graph = createGraph
    try {
      graph.execute(ExecutionConfiguration.withSignalThreshold(0))
      val productOfStates = graph.aggregate(new ProductOfStates[Double]).getOrElse(0.0)
      math.abs(productOfStates - 1.0) <= 0.0001
    } finally {
      graph.shutdown
    }
  }

  def createSudokuGraph(system: ActorSystem) = {
    val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(system.name).build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    graph.removeVertex(1)
    graph.addVertex(new SudokuCell(1, None))
    graph.addVertex(new SudokuCell(2, None))
    graph
  }

  "CountVertices" should "count the number of PageRank vertices correctly" in {
    val system = TestConfig.actorSystem()
    val graph = createSudokuGraph(system)
    try {
      val numberOfPRVertices = graph.aggregate(new CountVertices[PageRankVertex[Any]])
      numberOfPRVertices === 2
    } finally {
      graph.shutdown
    }
  }

  it should "count the number of SudokuCell vertices correctly" in {
    val system = TestConfig.actorSystem()
    val graph = createSudokuGraph(system)
    try {
      val numberOfSCVertices = graph.aggregate(new CountVertices[SudokuCell])
      numberOfSCVertices === 1
    } finally {
      graph.shutdown
    }
  }

  val idSet = (1 to 1000).toSet
  def createPageRankGraph(system: ActorSystem) = {
    val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(system.name).build
    for (id <- idSet) {
      graph.addVertex(new PageRankVertex(id))
    }
    graph
  }

  "SampleVertexIds" should "sample 0 vertex ids correctly" in {
    val system = TestConfig.actorSystem()
    val graph = createPageRankGraph(system)
    try {
      val vertexSample = graph.aggregate(new SampleVertexIds(0))
      vertexSample.size === 0
    } finally {
      graph.shutdown
    }
  }

  it should "sample 50 vertex ids correctly" in {
    val system = TestConfig.actorSystem()
    val graph = createPageRankGraph(system)
    try {
      val vertexSample = graph.aggregate(new SampleVertexIds(50))
      vertexSample.size === 50
      vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
    } finally {
      graph.shutdown
    }
  }

  it should "sample 1000 vertex ids correctly" in {
    val system = TestConfig.actorSystem()
    val graph = createPageRankGraph(system)
    try {
      val vertexSample = graph.aggregate(new SampleVertexIds(1000))
      vertexSample.size === 1000
      vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
    } finally {
      graph.shutdown
    }
  }

  "TopKFinder" should "find the largest vertices in the right order" in {
    val system = TestConfig.actorSystem()
    def createGraph = {
      val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(system.name).build
      graph.addVertex(new PageRankVertex(1, 0.3))
      graph.addVertex(new PageRankVertex(3, 0.2))
      graph.addVertex(new PageRankVertex(2, 0.1))
      graph.addVertex(new PageRankVertex(4, 0.4))
      graph
    }
    val graph = createGraph
    try {
      val largestVertices = graph.aggregate(new TopKFinder[Double](2))
      largestVertices.toSeq == Array[(Int, Double)]((2, 0.9), (3, 0.8)).toSeq
    } finally {
      graph.shutdown
    }
  }

}
