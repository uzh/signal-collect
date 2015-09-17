/*
 *  @author Bharath Kumar
 *  @author Philip Stutz
 *
 *  Copyright 2015 iHealth Technologies
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
 */

package com.signalcollect

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.signalcollect.ClusterTestUtils._
import com.signalcollect.examples.{PageRankEdge, PageRankVertex}
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeProvisionerActor, RetrieveNodeActors}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class ClusterPageRankSpecMultiJvmNode1 extends ClusterPageRankSpec

class ClusterPageRankSpecMultiJvmNode2 extends ClusterPageRankSpec

class ClusterPageRankSpecMultiJvmNode3 extends ClusterPageRankSpec

object ClusterPageRankConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
  val node1 = role("node1")
  val node2 = role("node2")

  val clusterName = "ClusterPageRankSpec"
  val seedPort = 2558

  nodeConfig(provisioner) {
    TestClusterConfig.provisionerCommonConfig(seedPort)
  }

  commonConfig {
    val mappingsConfig =
      """akka.actor.kryo.mappings {
        |  "com.signalcollect.ModularAggregator" = 133,
        |  "com.signalcollect.ClusterPageRankSpec$$anonfun$2" = 134,
        |  "com.signalcollect.ClusterPageRankSpec$$anonfun$3" = 135,
        |  "com.signalcollect.ClusterPageRankSpec$$anonfun$4" = 136,
        |  "com.signalcollect.ClusterPageRankSpec$$anonfun$5" = 137
        |    }""".stripMargin

    TestClusterConfig.nodeCommonConfig(clusterName, seedPort, mappingsConfig)
  }
}

class ClusterPageRankSpec extends MultiNodeSpec(ClusterPageRankConfig) with STMultiNodeSpec
with ImplicitSender with ScalaFutures {

  import ClusterPageRankConfig._

  override def initialParticipants = roles.size

  val workers = roles.size

  override def atStartup() = println("Starting")

  override def afterTermination() = println("Terminated")

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  def buildPageRankGraph(graph: Graph[Any, Any], edgeTuples: Traversable[Tuple2[Int, Int]]): Graph[Any, Any] = {
    edgeTuples foreach {
      case (sourceId: Int, targetId: Int) =>
        graph.addVertex(new PageRankVertex(sourceId, 0.85))
        graph.addVertex(new PageRankVertex(targetId, 0.85))
        graph.addEdge(sourceId, new PageRankEdge(targetId))
    }
    graph
  }

  val pageRankFiveCycleVerifier: Vertex[_, _, _, _] => Boolean = v => {
    val state = v.state.asInstanceOf[Double]
    val expectedState = 1.0
    val correct = (state - expectedState).abs < 0.01
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }

  val pageRankFiveStarVerifier: (Vertex[_, _, _, _]) => Boolean = v => {
    val state = v.state.asInstanceOf[Double]
    val expectedState = if (v.id == 4.0) 0.66 else 0.15
    val correct = (state - expectedState).abs < 0.00001
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }

  val pageRankTwoOnTwoGridVerifier: Vertex[_, _, _, _] => Boolean = v => {
    val state = v.state.asInstanceOf[Double]
    val expectedState = 1.0
    val correct = (state - expectedState).abs < 0.01
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }


  val pageRankTorusVerifier: Vertex[_, _, _, _] => Boolean = v => {
    val state = v.state.asInstanceOf[Double]
    val expectedState = 1.0
    val correct = (state - expectedState).abs < 0.01
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }

  "PageRank algorithm" must {
    implicit val timeout = Timeout(30.seconds)

    "deliver correct results on a 5-cycle graph" in within(100.seconds) {
      val prefix = TestConfig.prefix
      val fiveCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))

      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system).withActorNamePrefix(prefix)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = pageRankFiveCycleVerifier, buildGraph = buildPageRankGraph(_, fiveCycleEdges),
            signalThreshold = 0.001) shouldBe true
        }
      }
      enterBarrier("PageRank - test1 done")
    }

    "deliver correct results on a 5-star graph" in {
      val prefix = TestConfig.prefix
      val fiveStarEdges = List((0, 4), (1, 4), (2, 4), (3, 4))

      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = pageRankFiveStarVerifier, buildGraph = buildPageRankGraph(_, fiveStarEdges)) shouldBe true
        }
      }
      enterBarrier("PageRank - test2 done")
    }

    "deliver correct results on a 2*2 symmetric grid" in {
      val prefix = TestConfig.prefix
      val symmetricTwoOnTwoGridEdges = new Grid(2, 2)

      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = pageRankTwoOnTwoGridVerifier, buildGraph = buildPageRankGraph(_, symmetricTwoOnTwoGridEdges), signalThreshold = 0.001) shouldBe true
        }
      }
      enterBarrier("PageRank - test3 done")
    }

    //    "deliver correct results on a 5*5 torus" in {
    //      val prefix = TestConfig.prefix
    //      val symmetricTorusEdges = new Torus(5, 5)
    //
    //      runOn(provisioner) {
    //        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
    //          prefix, workers), "ClusterMasterBootstrap")
    //        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
    //        whenReady(nodeActorsFuture) { nodeActors =>
    //          assert(nodeActors.length == workers)
    //          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
    //            .withPreallocatedNodes(nodeActors).build)
    //          test(graphProviders = computeGraphFactories, verify = pageRankTorusVerifier, buildGraph = buildPageRankGraph(_, symmetricTorusEdges), signalThreshold = 0.001) shouldBe true
    //        }
    //      }
    //      enterBarrier("PageRank - test4 done")
    //    }
    //    TestConfig.printStats()
  }
  enterBarrier("PageRank - all tests done")
}
