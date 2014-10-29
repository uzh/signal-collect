/*
 *  @author Philip Stutz
 *  @author Thomas Keller
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
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GlobalTerminationDetection
import com.signalcollect.Graph
import com.signalcollect.GraphBuilder
import com.signalcollect.SumOfStates
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.TerminationReason
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import org.specs2.runner.JUnitRunner
import com.signalcollect.DataGraphVertex
import com.signalcollect.StateForwarderEdge
import com.signalcollect.DataFlowVertex
import com.signalcollect.GraphEditor
import akka.event.Logging

class CountingVertex(id: Int) extends DataGraphVertex(id, (0, 0)) {
  type Signal = Int
  def collect = (state._1, state._2 + 1)

  override def deliverSignalWithSourceId(signal: Any, sourceId: Any, graphEditor: GraphEditor[Any, Any]): Boolean = {
    state = (state._1 + 1, state._2)
    super.deliverSignalWithSourceId(signal, sourceId, graphEditor)
  }
}

@RunWith(classOf[JUnitRunner])
class ComputationTerminationSpec extends SpecificationWithJUnit with Mockito {

  def createPageRankCircleGraph(vertices: Int): Graph[Any, Any] = {
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

  def createCountingCircleGraph(vertices: Int): Graph[Any, Any] = {
    val graph = GraphBuilder.build //.withLoggingLevel(Logging.DebugLevel)
    val idSet = (1 to vertices).toSet
    for (id <- idSet) {
      graph.addVertex(new CountingVertex(id))
    }
    for (id <- idSet) {
      graph.addEdge(id, new StateForwarderEdge((id % vertices) + 1))
    }
    graph
  }

  sequential

  "Eager convergence detection" should {
    "not suffer from spurious idle detections" in {
      val g = createCountingCircleGraph(10)
      try {
        val steps = 2000
        g.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous).withStepsLimit(steps))
        g.foreachVertex(_.state === (steps, steps))
      } finally {
        g.shutdown
      }
      true
    }
  }

  "Steps limit" should {

    "work for synchronous computations" in {
      var allResultsCorrect = true
      for (i <- 1 to 10) {
        val graph = createPageRankCircleGraph(1000)
        try {
          val execConfig = ExecutionConfiguration
            .withSignalThreshold(0)
            .withStepsLimit(1)
            .withExecutionMode(ExecutionMode.Synchronous)
          val info = graph.execute(execConfig)
          val state = graph.forVertexWithId(1, (v: PageRankVertex[Any]) => v.state)
          info.executionStatistics.terminationReason === TerminationReason.ComputationStepLimitReached
          allResultsCorrect &= state === 0.2775
        } finally {
          graph.shutdown
        }
      }
      allResultsCorrect === true
    }
  }

  //  "Convergence detection" should {
  //    "work for asynchronous computations with one worker" in {
  //      val graph = createCircleGraph(3, Some(1))
  //      try {
  //        val info = graph.execute(ExecutionConfiguration.withSignalThreshold(0.0001))
  //        val state = graph.forVertexWithId(1, (v: PageRankVertex[Any]) => v.state)
  //        state > 0.99
  //        val aggregate = graph.aggregate(new SumOfStates[Double]).get
  //        if (info.executionStatistics.terminationReason != TerminationReason.Converged) {
  //          println("Computation ended for the wrong reason: " + info.executionStatistics.terminationReason)
  //        }
  //        aggregate > 2.99 && info.executionStatistics.terminationReason == TerminationReason.Converged
  //      } finally {
  //        graph.shutdown
  //      }
  //    }
  //  }

  "Global convergence" should {

    "work for synchronous computations" in {
      val graph = createPageRankCircleGraph(30)
      try {
        case object GlobalTermination extends GlobalTerminationDetection[Any, Any] {
          override val aggregationInterval: Long = 1
          def shouldTerminate(g: Graph[Any, Any]) = {
            val sum = g.aggregate(new SumOfStates[Double])
            sum.isDefined && sum.get > 20.0 && sum.get < 29.0
          }
        }
        val execConfig = ExecutionConfiguration
          .withSignalThreshold(0)
          .withGlobalTerminationDetection(GlobalTermination)
          .withExecutionMode(ExecutionMode.Synchronous)
        val info = graph.execute(execConfig)
        val state = graph.forVertexWithId(1, (v: PageRankVertex[Any]) => v.state)
        val aggregate = graph.aggregate(new SumOfStates[Double]).get
        aggregate > 20.0 && aggregate < 29.0 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
      } finally {
        graph.shutdown
      }
    }

    "work for asynchronous computations" in {
      val graph = createPageRankCircleGraph(100)
      try {
        case object GlobalTermination extends GlobalTerminationDetection[Any, Any] {
          override val aggregationInterval: Long = 1
          def shouldTerminate(g: Graph[Any, Any]) = {
            val sum = g.aggregate(new SumOfStates[Double])
            sum.isDefined && sum.get > 20.0
          }
        }
        val execConfig = ExecutionConfiguration
          .withSignalThreshold(0)
          .withGlobalTerminationDetection(GlobalTermination)
        val info = graph.execute(execConfig)
        val state = graph.forVertexWithId(1, (v: PageRankVertex[Any]) => v.state)
        val aggregate = graph.aggregate(new SumOfStates[Double]).get
        if (aggregate <= 20.0) {
          println("Computation ended before global condition was met.")
        }
        if (aggregate > 99.99999999) {
          println("Computation converged completely instead of ending when the global constraint was met: " + aggregate)
        }
        if (info.executionStatistics.terminationReason != TerminationReason.GlobalConstraintMet) {
          println("Computation ended for the wrong reason: " + info.executionStatistics.terminationReason)
        }
        aggregate > 20.0 && aggregate < 99.99999999 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
      } finally {
        graph.shutdown
      }
    }
  }

}
