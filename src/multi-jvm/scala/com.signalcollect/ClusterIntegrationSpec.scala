/*
 *  @author Bharath Kumar
 *
 *  Copyright 2014 University of Zurich
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
import akka.cluster.Cluster
import akka.event.Logging
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.signalcollect.configuration.{Akka, ExecutionMode}
import com.signalcollect.examples.{PageRankEdge, PageRankVertex}
import com.signalcollect.interfaces.ModularAggregationOperation
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeProvisionerActor, RetrieveNodeActors}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class ClusterIntegrationSpecMultiJvmNode1 extends ClusterIntegrationSpec

class ClusterIntegrationSpecMultiJvmNode2 extends ClusterIntegrationSpec

class ModularAggregator(verify: Vertex[_, _, _, _] => Boolean) extends ModularAggregationOperation[Boolean] {
    val neutralElement = true

    def aggregate(a: Boolean, b: Boolean): Boolean = a && b

    def extract(v: Vertex[_, _, _, _]): Boolean = verify(v)
}

object ClusterIntegrationConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
  val node1 = role("node1")

  val nodeConfig = ConfigFactory.load()
  val seedIp = nodeConfig.getString("akka.clustering.seed-ip")
  val seedPort = nodeConfig.getInt("akka.clustering.seed-port")
  val clusterName = "ClusterIntegrationSpec"

  def nodeList = Seq(provisioner, node1)

  val akkaConfig = Akka.config(serializeMessages = Some(false),
    loggingLevel = Some(Logging.WarningLevel),
    kryoRegistrations = List.empty,
    kryoInitializer = Some("com.signalcollect.configuration.TestKryoInit"))

  nodeConfig(provisioner) {
    ConfigFactory.parseString(
      s"""akka.remote.netty.tcp.port=$seedPort""".stripMargin)
  }

  commonConfig{
    val mappingsConfig = """        akka.actor.kryo.mappings {
                           |         "com.signalcollect.ModularAggregator" = 133,
                           |          "com.signalcollect.ClusterIntegrationSpec$$anonfun$2" = 134
                           |          }""".stripMargin
    ConfigFactory.parseString(
      s"""akka.actor.kryo.idstrategy=incremental
         |akka.cluster.seed-nodes=["akka.tcp://"${clusterName}"@"${seedIp}":"${seedPort}]""".stripMargin)
      .withFallback(ConfigFactory.parseString(mappingsConfig))
      .withFallback(akkaConfig)
  }
}

class ClusterIntegrationSpec extends MultiNodeSpec(ClusterIntegrationConfig) with STMultiNodeSpec
with ImplicitSender with ScalaFutures {

  import ClusterIntegrationConfig._

  override def initialParticipants = roles.size

  val workers = roles.size
  val provisionerAddress = node(provisioner).address
  val node1Address = node(node1).address
  val idleDetectionPropagationDelayInMilliseconds = 500

  override def atStartup() = println("STARTING UP!")

  override def afterTermination() = println("TERMINATION!")

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  //TODO: Its thorwing error for `OptimizedAsynchronous`, look into it
  val executionModes = List(ExecutionMode.Synchronous) //, ExecutionMode.OptimizedAsynchronous)

  def test(graphProviders: List[() => Graph[Any, Any]], verify: Vertex[_, _, _, _] => Boolean, buildGraph: Graph[Any, Any] => Unit = (graph: Graph[Any, Any]) => (), signalThreshold: Double = 0.01, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ExecutionInformation[Any, Any]]]()

    for (executionMode <- executionModes) {
      for (graphProvider <- graphProviders) {
        val graph = graphProvider()
        try {
          buildGraph(graph)
          val stats = graph.execute(ExecutionConfiguration(executionMode = executionMode, signalThreshold = signalThreshold))
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

  "PageRank algorithm" must {

    "deliver correct results on a 5-cycle graph" in within(100.seconds) {
      val fiveCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))
      runOn(provisioner) {
        system.actorOf(Props(classOf[ClusterNodeProvisionerActor], idleDetectionPropagationDelayInMilliseconds,
          "ClusterMasterBootstrap", workers), "ClusterMasterBootstrap")
      }
      enterBarrier("provisioner up")

      runOn(node1) {
        Cluster(system).join(node1Address)
      }
      enterBarrier("node1 started")

      runOn(provisioner) {
        implicit val timeout = Timeout(300.seconds)
        val masterActor = system.actorSelection(node(provisioner) / "user" / "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
          val computeGraphFactories: List[() => Graph[Any, Any]] = List(() => GraphBuilder.withActorSystem(system)
            .withPreallocatedNodes(nodeActors).build)
          test(graphProviders = computeGraphFactories, verify = pageRankFiveCycleVerifier, buildGraph = buildPageRankGraph(_, fiveCycleEdges),
            signalThreshold = 0.001) shouldBe true
        }
      }
      enterBarrier("test1 done!")
    }
  }
}
