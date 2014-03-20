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

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.CountVertices
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GraphBuilder
import com.signalcollect.ProductOfStates
import com.signalcollect.SampleVertexIds
import com.signalcollect.SumOfStates
import com.signalcollect.TopKFinder
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.SudokuCell
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AggregationOperationsSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "SumOfStates" should {
    def createGraph = {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new SudokuCell(1, None))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph
    }

    "sum all states correctly" in {
      val graph = createGraph
      try {
        graph.execute(ExecutionConfiguration.withSignalThreshold(0))
        val sumOfStates = graph.aggregate(new SumOfStates[Double]).getOrElse(0.0)
        math.abs(sumOfStates - 2.0) <= 0.0001
      } finally {
        graph.shutdown
      }
    }

  }

  "ProductOfStates" should {
    def createGraph = {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new SudokuCell(1, None))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph
    }

    "multiply all states correctly" in {
      val graph = createGraph
      try {
        graph.execute(ExecutionConfiguration.withSignalThreshold(0))
        val productOfStates = graph.aggregate(new ProductOfStates[Double]).getOrElse(0.0)
        math.abs(productOfStates - 1.0) <= 0.0001
      } finally {
        graph.shutdown
      }
    }
  }

  "CountVertices" should {
    def createGraph = {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.removeVertex(1)
      graph.addVertex(new SudokuCell(1, None))
      graph.addVertex(new SudokuCell(2, None))
      graph
    }

    "count the number of PageRank vertices correctly" in {
      val graph = createGraph
      try {
        val numberOfPRVertices = graph.aggregate(new CountVertices[PageRankVertex[Any]])
        numberOfPRVertices === 2
      } finally {
        graph.shutdown
      }
    }

    "count the number of SudokuCell vertices correctly" in {
      val graph = createGraph
      try {
        val numberOfSCVertices = graph.aggregate(new CountVertices[SudokuCell])
        numberOfSCVertices === 1
      } finally {
        graph.shutdown
      }
    }
  }

  "SampleVertexIds" should {
    val idSet = (1 to 1000).toSet
    def createGraph = {
      val graph = GraphBuilder.build
      for (id <- idSet) {
        graph.addVertex(new PageRankVertex(id))
      }
      graph
    }

    "sample 0 vertex ids correctly" in {
      val graph = createGraph
      try {
        val vertexSample = graph.aggregate(new SampleVertexIds(0))
        vertexSample.size === 0
      } finally {
        graph.shutdown
      }
    }

    "sample 50 vertex ids correctly" in {
      val graph = createGraph
      try {
        val vertexSample = graph.aggregate(new SampleVertexIds(50))
        vertexSample.size === 50
        vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
      } finally {
        graph.shutdown
      }
    }

    "sample 50 vertex ids correctly" in {
      val graph = createGraph
      try {
        val vertexSample = graph.aggregate(new SampleVertexIds(1000))
        vertexSample.size === 1000
        vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
      } finally {
        graph.shutdown
      }
    }
  }

  "TopKFinder" should {
    def createGraph = {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1, 0.3))
      graph.addVertex(new PageRankVertex(3, 0.2))
      graph.addVertex(new PageRankVertex(2, 0.1))
      graph.addVertex(new PageRankVertex(4, 0.4))
      graph
    }

    "find the largest vertices in the right order" in {
      val graph = createGraph
      try {
        val largestVertices = graph.aggregate(new TopKFinder[Double](2))
        largestVertices.toSeq == Array[(Int, Double)]((2, 0.9), (3, 0.8)).toSeq
      } finally {
        graph.shutdown
      }
    }

  }

}