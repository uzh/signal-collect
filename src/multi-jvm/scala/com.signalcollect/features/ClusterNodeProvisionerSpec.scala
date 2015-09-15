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

package com.signalcollect.features

import akka.actor.{ActorRef, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeProvisionerActor, RetrieveNodeActors}
import com.signalcollect.{TestConfig, Graph, GraphBuilder, STMultiNodeSpec}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class ClusterNodeProvisionerSpecMultiJvmNode1 extends ClusterNodeProvisionerSpec

class ClusterNodeProvisionerSpecMultiJvmNode2 extends ClusterNodeProvisionerSpec

class ClusterNodeProvisionerSpecMultiJvmNode3 extends ClusterNodeProvisionerSpec

object ClusterNodeProvisionerConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
  val node1 = role("node1")
  val node2 = role("node2")

  val nodeConfig = ConfigFactory.load()
  val seedIp = nodeConfig.getString("akka.clustering.seed-ip")
  val seedPort = nodeConfig.getInt("akka.clustering.seed-port")
  val clusterName = "ClusterNodeProvisionerSpec"

  nodeConfig(provisioner) {
    ConfigFactory.parseString(
      s"""akka.remote.netty.tcp.port=$seedPort""".stripMargin)
  }

  // this configuration will be used for all nodes
  commonConfig(ConfigFactory.parseString(
    s"""akka.cluster.seed-nodes=["akka.tcp://"${clusterName}"@"${seedIp}":"${seedPort}]""".stripMargin)
    .withFallback(ConfigFactory.load()))
}

class ClusterNodeProvisionerSpec extends MultiNodeSpec(ClusterNodeProvisionerConfig) with STMultiNodeSpec
with ImplicitSender with ScalaFutures {

  import ClusterNodeProvisionerConfig._

  override def initialParticipants = roles.size

  override def atStartup() = println("STARTING UP!")

  override def afterTermination() = println("TERMINATION!")

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  val workers = 3
  val node1Address = node(node1).address
  val node2Address = node(node2).address
  val idleDetectionPropagationDelayInMilliseconds = 500
  val prefix = TestConfig.prefix
  
  "SignalCollect" should {
    "get the cluster up" in {
      runOn(provisioner) {
        system.actorOf(Props(classOf[ClusterNodeProvisionerActor], idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
      }
      enterBarrier("provisioner up")

      runOn(node1) {
        Cluster(system).join(node1Address)
      }
      enterBarrier("node1 started")

      runOn(node2) {
        Cluster(system).join(node2Address)
      }
      enterBarrier("node2 started")

      runOn(provisioner) {
        implicit val timeout = Timeout(300.seconds)
        val masterActor = system.actorSelection(node(provisioner) / "user" / "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.size == workers)

          val graph = GraphBuilder.withActorSystem(system).withPreallocatedNodes(nodeActors).build
          try {
            val stats = graph.execute
            assert(stats.individualWorkerStatistics.length == Runtime.getRuntime.availableProcessors * workers)
          } finally {
            graph.shutdown
          }
        }
      }
      enterBarrier("all nodes up!")
    }
  }
}



