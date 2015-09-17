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
import akka.event.Logging
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.signalcollect.configuration.{Akka, ExecutionMode}
import com.signalcollect.examples.{Location, PageRankEdge, PageRankVertex, Path}
import com.signalcollect.interfaces.ModularAggregationOperation
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeProvisionerActor, RetrieveNodeActors}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class ClusterIntegrationSpecMultiJvmNode1 extends ClusterIntegrationSpec

class ClusterIntegrationSpecMultiJvmNode2 extends ClusterIntegrationSpec

class ClusterIntegrationSpecMultiJvmNode3 extends ClusterIntegrationSpec

class ModularAggregator(verify: Vertex[_, _, _, _] => Boolean) extends ModularAggregationOperation[Boolean] {
  val neutralElement = true

  def aggregate(a: Boolean, b: Boolean): Boolean = a && b

  def extract(v: Vertex[_, _, _, _]): Boolean = verify(v)
}

object ClusterIntegrationConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
  val node1 = role("node1")
  val node2 = role("node2")

  val clusterName = "ClusterIntegrationSpec"
  val seedPort = 2558

  val akkaConfig = Akka.config(serializeMessages = Some(false),
    loggingLevel = Some(Logging.WarningLevel),
    kryoRegistrations = List.empty,
    kryoInitializer = Some("com.signalcollect.configuration.TestKryoInit"))

  nodeConfig(provisioner) {
    TestClusterConfig.provisionerCommonConfig(seedPort)
  }

  commonConfig {
    val mappingsConfig = """akka.actor.kryo.mappings {
                           |  "com.signalcollect.ModularAggregator" = 133,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$2" = 134,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$3" = 135,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$4" = 136,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$5" = 137,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$6" = 138,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$7" = 139,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$12" = 140,
                           |  "com.signalcollect.VerifiedColoredVertex" = 141,
                           |  "com.signalcollect.StateForwarderEdge" = 142,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$16" = 143,
                           |  "com.signalcollect.ClusterIntegrationSpec$$anonfun$17" = 144
                           |    }""".stripMargin
    ConfigFactory.parseString(
      s"""akka.actor.kryo.idstrategy=incremental
         |akka.testconductor.barrier-timeout=60s
       """.stripMargin)
      .withFallback(TestClusterConfig.nodeCommonConfig(clusterName, seedPort))
      .withFallback(ConfigFactory.parseString(mappingsConfig))
      .withFallback(akkaConfig)
  }
}

class ClusterIntegrationSpec extends MultiNodeSpec(ClusterIntegrationConfig) with STMultiNodeSpec
with ImplicitSender with ScalaFutures {

  import ClusterIntegrationConfig._

  override def initialParticipants = roles.size

  val workers = roles.size

  override def atStartup() = println("Starting")

  override def afterTermination() = println("Terminated")

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  val executionModes = List(ExecutionMode.ContinuousAsynchronous)

  def test(graphProviders: List[() => Graph[Any, Any]], verify: Vertex[_, _, _, _] => Boolean, buildGraph: Graph[Any, Any] => Unit = (graph: Graph[Any, Any]) => (), signalThreshold: Double = 0.01, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ExecutionInformation[Any, Any]]]()

    for (executionMode <- executionModes) {
      for (graphProvider <- graphProviders) {
        val graph = graphProvider()
        try {
          buildGraph(graph)
          graph.awaitIdle
          val stats = graph.execute(ExecutionConfiguration(executionMode = executionMode, signalThreshold = signalThreshold))
          graph.awaitIdle
          correct &= graph.aggregate(new ModularAggregator(verify))
          if (!correct) {
            System.err.println("Test failed. Computation stats: " + stats)
          }
        } finally {
          graph.shutdown
        }
      }
    }
    correct
  }

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
    TestConfig.printStats()

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
    TestConfig.printStats()

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
    TestConfig.printStats()

    "deliver correct results on a 5*5 torus" in {
      val prefix = TestConfig.prefix
      val symmetricTorusEdges = new Torus(5, 5)

      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = pageRankTorusVerifier, buildGraph = buildPageRankGraph(_, symmetricTorusEdges), signalThreshold = 0.001) shouldBe true
        }
      }
      enterBarrier("PageRank - test4 done")
    }
    TestConfig.printStats()
  }
  enterBarrier("PageRank - all tests done")

  val vertexColoringVerifier: Vertex[_, _, _, _] => Boolean = v => {
    v match {
      case v: VerifiedColoredVertex =>
        val verified = !v.publicMostRecentSignals.iterator.contains(v.state)
        if (!verified) {
          System.err.println("Vertex Coloring: " + v + " has the same color as one of its neighbors.\n" +
            "Most recent signals received: " + v.publicMostRecentSignals + "\n" +
            "Score signal: " + v.scoreSignal)
        }
        verified
      case other =>
        System.err.println("Vertex " + other + " is not of type VerifiedColoredVertex"); false
    }
  }

  def buildVertexColoringGraph(numColors: Int, graph: Graph[Any, Any], edgeTuples: Traversable[Tuple2[Int, Int]]): Graph[Any, Any] = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        graph.addVertex(new VerifiedColoredVertex(sourceId, numColors))
        graph.addVertex(new VerifiedColoredVertex(targetId, numColors))
        graph.addEdge(sourceId, new StateForwarderEdge(targetId))
    }
    graph
  }

  "VertexColoring algorithm" should {
    implicit val timeout = Timeout(30.seconds)

    "deliver correct results on a symmetric 4-cycle" in {
      val prefix = TestConfig.prefix
      val symmetricFourCycleEdges = List((0, 1), (1, 0), (1, 2), (2, 1), (2, 3), (3, 2), (3, 0), (0, 3))

      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFourCycleEdges)) shouldBe true
        }
      }
      enterBarrier("VertexColoring - test1 done")
    }
    TestConfig.printStats()

    "deliver correct results on a symmetric 5-star" in {
      val prefix = TestConfig.prefix
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFiveStarEdges)) shouldBe true
        }
      }
      enterBarrier("VertexColoring - test2 done")
    }
    TestConfig.printStats()

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
          test(graphProviders = computeGraphFactories, verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricTwoOnTwoGridEdges)) shouldBe true
        }
      }
      enterBarrier("VertexColoring - test3 done")
    }
    TestConfig.printStats()
  }
  enterBarrier("VertexColoring - all tests done")

  def buildSsspGraph(pathSourceId: Any, graph: Graph[Any, Any], edgeTuples: Traversable[Tuple2[Int, Int]]): Graph[Any, Any] = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        if (sourceId.equals(pathSourceId)) {
          graph.addVertex(new Location(sourceId, Some(0)))
        } else {
          graph.addVertex(new Location(sourceId, None))
        }
        if (targetId.equals(pathSourceId)) {
          graph.addVertex(new Location(targetId, Some(0)))
        } else {
          graph.addVertex(new Location(targetId, None))
        }
        graph.addEdge(sourceId, new Path(targetId))
    }
    graph
  }

  val ssspSymmetricsFourCycleVerifier: Vertex[_, _, _, _] => Boolean = v => {
    val state = v.state.asInstanceOf[Option[Int]].get
    val expectedState = v.id
    val correct = state == expectedState
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }

  val ssspSymmetricFiveStarVerifier: Vertex[_, _, _, _] => Boolean = v => {
    val state = v.state.asInstanceOf[Option[Int]].get
    val expectedState = if (v.id == 4) 0 else 1
    val correct = state == expectedState
    if (!correct) {
      System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + ", actual state=" + state)
    }
    correct
  }

  "SSSP algorithm" should {
    implicit val timeout = Timeout(30.seconds)

    "deliver correct results on a symmetric 4-cycle" in {
      val prefix = TestConfig.prefix
      val symmetricFourCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 0))
      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = ssspSymmetricsFourCycleVerifier, buildGraph = buildSsspGraph(0, _, symmetricFourCycleEdges)) shouldBe true
        }
      }
      enterBarrier("SSSP - test1 done")
    }
    TestConfig.printStats()

    "deliver correct results on a symmetric 5-star" in {
      val prefix = TestConfig.prefix
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      runOn(provisioner) {
        val masterActor = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], TestClusterConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = ssspSymmetricFiveStarVerifier, buildGraph = buildSsspGraph(4, _, symmetricFiveStarEdges)) shouldBe true
        }
      }
      enterBarrier("SSSP - test2 done")
    }
    TestConfig.printStats()
  }
  enterBarrier("SSSP - all tests done")
}
