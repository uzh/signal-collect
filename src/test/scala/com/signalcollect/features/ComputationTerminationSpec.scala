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
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.TerminationReason

@RunWith(classOf[JUnitRunner])
class ComputationTerminationSpec extends SpecificationWithJUnit with Mockito {

  def createCircleGraph(vertices: Int): Graph = {
    val graph = GraphBuilder.build
    val idSet = (1 to vertices).toSet
    for (id <- idSet) {
      graph.addVertex(new PageRankVertex(id))
    }
    for (id <- idSet) {
      graph.addEdge(id, new PageRankEdge((id % vertices) + 1))
    }
    graph
  }
  
  sequential

  "Time limit" should {

    "work for asynchronous computations" in {
      val graph = createCircleGraph(1000)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(30)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      state > 0.15 && state < 0.999999999999999 && info.executionStatistics.terminationReason == TerminationReason.TimeLimitReached
    }

    "work for synchronous computations" in {
      val graph = createCircleGraph(100)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(30)
        .withExecutionMode(ExecutionMode.Synchronous)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      state > 0.15 && state < 0.999999999999999 && info.executionStatistics.terminationReason == TerminationReason.TimeLimitReached
    }
  }

  "Steps limit" should {

    "work for synchronous computations" in {
      val graph = createCircleGraph(1000)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withStepsLimit(1)
        .withExecutionMode(ExecutionMode.Synchronous)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      state == 0.2775 && info.executionStatistics.terminationReason == TerminationReason.ComputationStepLimitReached
    }
  }

  "Global convergence" should {

    "work for synchronous computations" in {
      val graph = createCircleGraph(30)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1) {
        def shouldTerminate(sum: Option[Double]): Boolean = {
          sum.isDefined && sum.get > 20.0 && sum.get < 29.0
        }
      }
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
        .withExecutionMode(ExecutionMode.Synchronous)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      aggregate > 20.0 && aggregate < 29.0 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
    }

    "work for asynchronous computations" in {
      val graph = createCircleGraph(100)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1l) {
        def shouldTerminate(sum: Option[Double]): Boolean = {
          sum.isDefined && sum.get > 20.0
        }
      }
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      if (aggregate <= 20.0) {
        println("Computation ended before global condition was met.")
      }
      if (aggregate > 99.999) {
        println("Computation converged completely instead of ending when the global constraint was met: " + aggregate)
      }
      if (info.executionStatistics.terminationReason != TerminationReason.GlobalConstraintMet) {
        println("Computation ended for the wrong reason: " + info.executionStatistics.terminationReason)
      }
      aggregate > 20.0 && aggregate < 99.999 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
    }
  }

}