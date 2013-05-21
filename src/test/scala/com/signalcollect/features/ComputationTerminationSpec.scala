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
import com.signalcollect.nodeprovisioning.Node
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import org.specs2.runner.JUnitRunner
import com.typesafe.config.Config
import akka.actor.ActorRef
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.Props
import com.signalcollect.nodeprovisioning.NodeActorCreator
import com.signalcollect.nodeprovisioning.DefaultNodeActor

@RunWith(classOf[JUnitRunner])
class ComputationTerminationSpec extends SpecificationWithJUnit with Mockito {

  def createCircleGraph(vertices: Int, numberOfWorkers: Option[Int] = None): Graph[Any, Any] = {
    val graph = {
      if (numberOfWorkers.isEmpty) {
        GraphBuilder.build
      } else {
        GraphBuilder.withNodeProvisioner(new LocalNodeProvisioner {
          override def getNodes(akkaConfig: Config): Array[ActorRef] = {
            val system = ActorSystemRegistry.retrieve("SignalCollect").getOrElse(throw new Exception("No actor system with name \"SignalCollect\" found!"))
            if (system != null) {
              val nodeControllerCreator = NodeActorCreator(0, None)
              val nodeController = system.actorOf(Props[DefaultNodeActor].withCreator(nodeControllerCreator.create), name = "DefaultNodeActor")
              Array[ActorRef](nodeController)
            } else {
              Array[ActorRef]()
            }
          }
        }).build
      }
    }
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
        val execConfig = ExecutionConfiguration
          .withSignalThreshold(0)
          .withStepsLimit(1)
          .withExecutionMode(ExecutionMode.Synchronous)
        val info = graph.execute(execConfig)
        val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
        graph.shutdown
        info.executionStatistics.terminationReason === TerminationReason.ComputationStepLimitReached
        allResultsCorrect &= state === 0.2775
      }
      allResultsCorrect === true
    }
  }

  "Convergence detection" should {

    "work for asynchronous computations with one worker" in {
      val graph = createCircleGraph(3, Some(1))
      val info = graph.execute(ExecutionConfiguration.withSignalThreshold(0.0001))
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      state > 0.99
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      if (info.executionStatistics.terminationReason != TerminationReason.Converged) {
        println("Computation ended for the wrong reason: "+info.executionStatistics.terminationReason)
      }
      graph.shutdown
      aggregate > 2.99 && info.executionStatistics.terminationReason == TerminationReason.Converged
    }
  }

  "Global convergence" should {

    "work for synchronous computations" in {
      val graph = createCircleGraph(30)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1, (sum: Option[Double]) =>
        sum.isDefined && sum.get > 20.0 && sum.get < 29.0)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
        .withExecutionMode(ExecutionMode.Synchronous)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      graph.shutdown
      aggregate > 20.0 && aggregate < 29.0 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
    }

    "work for asynchronous computations" in {
      val graph = createCircleGraph(100)
      val terminationCondition = new GlobalTerminationCondition(new SumOfStates[Double], 1l, (sum: Option[Double]) =>
        sum.isDefined && sum.get > 20.0)
      val execConfig = ExecutionConfiguration
        .withSignalThreshold(0)
        .withGlobalTerminationCondition(terminationCondition)
      val info = graph.execute(execConfig)
      val state = graph.forVertexWithId(1, (v: PageRankVertex) => v.state)
      val aggregate = graph.aggregate(new SumOfStates[Double]).get
      if (aggregate <= 20.0) {
        println("Computation ended before global condition was met.")
      }
      if (aggregate > 99.99999999) {
        println("Computation converged completely instead of ending when the global constraint was met: "+aggregate)
      }
      if (info.executionStatistics.terminationReason != TerminationReason.GlobalConstraintMet) {
        println("Computation ended for the wrong reason: "+info.executionStatistics.terminationReason)
      }
      graph.shutdown
      aggregate > 20.0 && aggregate < 99.99999999 && info.executionStatistics.terminationReason == TerminationReason.GlobalConstraintMet
    }
  }

}
