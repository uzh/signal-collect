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

  "Time limit" should {

    "work for asynchronous computations" in {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addVertex(new PageRankVertex(4))
      graph.addVertex(new PageRankVertex(5))
      graph.addVertex(new PageRankVertex(6))
      graph.addVertex(new PageRankVertex(7))
      graph.addEdge(new PageRankEdge(1, 2))
      graph.addEdge(new PageRankEdge(2, 3))
      graph.addEdge(new PageRankEdge(3, 4))
      graph.addEdge(new PageRankEdge(4, 5))
      graph.addEdge(new PageRankEdge(5, 6))
      graph.addEdge(new PageRankEdge(6, 7))
      graph.addEdge(new PageRankEdge(7, 1))
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(10)
      graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      state > 0.16 && state < 0.999999
    }

    "work for synchronous computations" in {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addVertex(new PageRankVertex(4))
      graph.addVertex(new PageRankVertex(5))
      graph.addVertex(new PageRankVertex(6))
      graph.addVertex(new PageRankVertex(7))
      graph.addEdge(new PageRankEdge(1, 2))
      graph.addEdge(new PageRankEdge(2, 3))
      graph.addEdge(new PageRankEdge(3, 4))
      graph.addEdge(new PageRankEdge(4, 5))
      graph.addEdge(new PageRankEdge(5, 6))
      graph.addEdge(new PageRankEdge(6, 7))
      graph.addEdge(new PageRankEdge(7, 1))
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withTimeLimit(10)
        .withExecutionMode(SynchronousExecutionMode)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      state > 0.16 && state < 0.999999 && info.executionStatistics.terminationReason == TimeLimitReached
    }
  }

  "Steps limit" should {

    "work for synchronous computations" in {
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addVertex(new PageRankVertex(4))
      graph.addVertex(new PageRankVertex(5))
      graph.addVertex(new PageRankVertex(6))
      graph.addVertex(new PageRankVertex(7))
      graph.addEdge(new PageRankEdge(1, 2))
      graph.addEdge(new PageRankEdge(2, 3))
      graph.addEdge(new PageRankEdge(3, 4))
      graph.addEdge(new PageRankEdge(4, 5))
      graph.addEdge(new PageRankEdge(5, 6))
      graph.addEdge(new PageRankEdge(6, 7))
      graph.addEdge(new PageRankEdge(7, 1))
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
      val graph = GraphBuilder.build
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addVertex(new PageRankVertex(4))
      graph.addVertex(new PageRankVertex(5))
      graph.addVertex(new PageRankVertex(6))
      graph.addVertex(new PageRankVertex(7))
      graph.addEdge(new PageRankEdge(1, 2))
      graph.addEdge(new PageRankEdge(2, 3))
      graph.addEdge(new PageRankEdge(3, 4))
      graph.addEdge(new PageRankEdge(4, 5))
      graph.addEdge(new PageRankEdge(5, 6))
      graph.addEdge(new PageRankEdge(6, 7))
      graph.addEdge(new PageRankEdge(7, 1))
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1) {
        def shouldTerminate(sum: Option[Double]): Boolean = {
          sum.isDefined && sum.get > 4.0 && sum.get < 4.5
        }
      }
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
        .withExecutionMode(SynchronousExecutionMode)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
      true
    }
    
//    "work for asynchronous computations" in {
//      val graph = GraphBuilder.build
//      graph.addVertex(new PageRankVertex(1))
//      graph.addVertex(new PageRankVertex(2))
//      graph.addVertex(new PageRankVertex(3))
//      graph.addVertex(new PageRankVertex(4))
//      graph.addVertex(new PageRankVertex(5))
//      graph.addVertex(new PageRankVertex(6))
//      graph.addVertex(new PageRankVertex(7))
//      graph.addEdge(new PageRankEdge(1, 2))
//      graph.addEdge(new PageRankEdge(2, 3))
//      graph.addEdge(new PageRankEdge(3, 4))
//      graph.addEdge(new PageRankEdge(4, 5))
//      graph.addEdge(new PageRankEdge(5, 6))
//      graph.addEdge(new PageRankEdge(6, 7))
//      graph.addEdge(new PageRankEdge(7, 1))
//      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 0) {
//        def shouldTerminate(sum: Option[Double]): Boolean = {
//          sum.isDefined && sum.get > 4.0 && sum.get < 4.5
//        }
//      }
//      val execConfig = ExecutionConfiguration
//        .withSignalThreshold(0)
//        .withGlobalTerminationCondition(terminationCondition)
//      val info = graph.execute(execConfig)
//      println(info)
//      graph.foreachVertex((println(_)))
//      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state).get
//      true
//    }
  }

}