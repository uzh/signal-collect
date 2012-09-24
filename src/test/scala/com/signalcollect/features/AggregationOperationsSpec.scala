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

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import java.util.Map.Entry
import com.signalcollect._
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.SudokuCell

@RunWith(classOf[JUnitRunner])
class AggregationOperationsSpec extends SpecificationWithJUnit with Mockito {

  "SumOfStates" should {
    val graph = GraphBuilder.build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new SudokuCell(1, None))
    graph.addEdge(1, new PageRankEdge(2))
    graph.addEdge(2, new PageRankEdge(1))
    graph.execute(ExecutionConfiguration.withSignalThreshold(0))

    "sum all states correctly" in {
      val sumOfStates = graph.aggregate(new SumOfStates[Double]).getOrElse(0.0)
      (sumOfStates - 2.0) <= 0.0001
    }
  }

  "ProductOfStates" should {
    val graph = GraphBuilder.build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new SudokuCell(1, None))
    graph.addEdge(1, new PageRankEdge(2))
    graph.addEdge(2, new PageRankEdge(1))
    graph.execute(ExecutionConfiguration.withSignalThreshold(0))

    "multiply all states correctly" in {
      val productOfStates = graph.aggregate(new ProductOfStates[Double]).getOrElse(0.0)
      (productOfStates - 1.0) <= 0.0001
    }
  }

  "CountVertices" should {
    val graph = GraphBuilder.build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    graph.addVertex(new SudokuCell(1, None))
    graph.removeVertex(1)

    "count the number of PageRank vertices correctly" in {
      val numberOfPRVertices = graph.aggregate(new CountVertices[PageRankVertex])
      (numberOfPRVertices - 2.0) <= 0.0001
    }

    "count the number of SudokuCell vertices correctly" in {
      val numberOfSCVertices = graph.aggregate(new CountVertices[SudokuCell])
      (numberOfSCVertices - 1.0) <= 0.0001
    }
  }

  "SampleVertexIds" should {
    val graph = GraphBuilder.build
    val idSet = (1 to 1000).toSet
    for (id <- idSet) {
      graph.addVertex(new PageRankVertex(id))
    }

    "sample 0 vertex ids correctly" in {
      val vertexSample = graph.aggregate(new SampleVertexIds(0))
      vertexSample.size == 0
    }

    "sample 50 vertex ids correclty" in {
      val vertexSample = graph.aggregate(new SampleVertexIds(50))
      vertexSample.size == 50 && vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
    }
    
    "sample 50 vertex ids correclty" in {
      val vertexSample = graph.aggregate(new SampleVertexIds(1000))
      vertexSample.size == 1000 && vertexSample.forall(id => idSet.contains(id.asInstanceOf[Int]))
    }
  }

}