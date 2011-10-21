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
class ComputationTerminationSpec extends SpecificationWithJUnit with Mockito {

  def createGraph(vertices: Int): Graph = {
      val graph = GraphBuilder.build
      val idSet = (1 to vertices).toSet
      for (id <- idSet) {
    	  graph.addVertex(new PageRankVertex(id)) 
      }
      for (id <- idSet) {
    	  graph.addEdge(new PageRankEdge(id, (id % vertices) + 1)) 
      }
      graph
  }
  
  "Time limit" should {

    "work for asynchronous computations" in {
      val graph = createGraph(1000)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(15)
      graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      state > 0.16 && state < 0.9999999999
    }

    "work for synchronous computations" in {
      val graph = createGraph(100)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(30)
        .withExecutionMode(SynchronousExecutionMode)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      state > 0.16 && state < 0.99999999999 && info.executionStatistics.terminationReason == TimeLimitReached
    }
  }

  "Steps limit" should {

    "work for synchronous computations" in {
      val graph = createGraph(1000)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withStepsLimit(1)
        .withExecutionMode(SynchronousExecutionMode)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      state == 0.2775 && info.executionStatistics.terminationReason == ComputationStepLimitReached
    }
  }

  "Global convergence" should {

    "work for synchronous computations" in {
      val graph = createGraph(30)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1) {
        def shouldTerminate(sum: Option[Double]): Boolean = {
          sum.isDefined && sum.get > 20.0 && sum.get < 29.0
        }
      }
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
        .withExecutionMode(SynchronousExecutionMode)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      aggregate > 20.0 && aggregate < 29.0
    }
    
    "work for asynchronous computations" in {
      val graph = createGraph(1000)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1l) {
        def shouldTerminate(sum: Option[Double]): Boolean = {
          sum.isDefined && sum.get > 700.0
        }
      }
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      aggregate > 700.0 && aggregate < 999.9999
    }
  }

}