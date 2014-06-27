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
import com.signalcollect.GlobalTerminationCondition
import com.signalcollect.Graph
import com.signalcollect.GraphBuilder
import com.signalcollect.SumOfStates
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.TerminationReason
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.interfaces.Node
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import org.specs2.runner.JUnitRunner
import com.typesafe.config.Config
import akka.actor.ActorRef
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.Props
import akka.actor.ActorSystem
import com.signalcollect.node.DefaultNodeActor

@RunWith(classOf[JUnitRunner])
class ComputationTerminationSpec extends SpecificationWithJUnit with Mockito {

  def createCircleGraph(vertices: Int): Graph[Any, Any] = {
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

  "Steps limit" should {

    "work for synchronous computations" in {
      var allResultsCorrect = true
      for (i <- 1 to 10) {
        val graph = createCircleGraph(1000)
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
      val graph = createCircleGraph(30)
      try {
        case object GlobalTermination extends GlobalTerminationCondition {
          type ResultType = Option[Double]
          override val aggregationInterval: Long = 1
          val aggregationOperation = new SumOfStates[Double]
          def shouldTerminate(sum: Option[Double]) = sum.isDefined && sum.get > 20.0 && sum.get < 29.0
        }
        val execConfig = ExecutionConfiguration
          .withSignalThreshold(0)
          .withGlobalTerminationCondition(GlobalTermination)
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
      val graph = createCircleGraph(100)
      try {
        case object GlobalTermination extends GlobalTerminationCondition {
          type ResultType = Option[Double]
          override val aggregationInterval: Long = 1
          val aggregationOperation = new SumOfStates[Double]
          def shouldTerminate(sum: Option[Double]) = sum.isDefined && sum.get > 20.0
        }
        val execConfig = ExecutionConfiguration
          .withSignalThreshold(0)
          .withGlobalTerminationCondition(GlobalTermination)
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
