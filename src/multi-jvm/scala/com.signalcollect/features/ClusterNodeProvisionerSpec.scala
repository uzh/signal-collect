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

import akka.cluster.Cluster
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeEntryPointTemplate, ClusterNodeProvisioner}
import com.signalcollect.{Graph, GraphBuilder, STMultiNodeSpec}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.duration._

class ClusterNodeProvisionerSpecMultiJvmNode1 extends ClusterNodeProvisionerSpec

class ClusterNodeProvisionerSpecMultiJvmNode2 extends ClusterNodeProvisionerSpec

//class ClusterNodeProvisionerSpecMultiJvmNode3 extends ClusterNodeProvisionerSpec

object ClusterNodeProvisionerConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
  val node1 = role("node1")
//  val node2 = role("node2")
  val nodeConfig = ConfigFactory.load()
  val seedIp = nodeConfig.getString("akka.clustering.seed-ip")
  val seedPort = nodeConfig.getInt("akka.clustering.seed-port")
  val clusterName = "ClusterNodeProvisionerSpec"

  val provisionerChildConfig = {
    ConfigFactory.parseString(
      s"""akka.clustering.name=$clusterName
          |akka.cluster.seed-nodes=["akka.tcp://$clusterName@$seedIp:$seedPort"]
          |akka.remote.netty.tcp.port=$seedPort""".stripMargin).withFallback(ConfigFactory.load())
  }

  val workerNodeConfig = {
    ConfigFactory.parseString(
      s"""akka.clustering.name=$clusterName
          |akka.cluster.seed-nodes=["akka.tcp://$clusterName@$seedIp:$seedPort"]
          |akka.cluster.roles = [node]
       """.stripMargin).withFallback(ConfigFactory.load())
  }

  nodeConfig(node1) {
      workerNodeConfig
    }
}

class ClusterNodeProvisionerSpec extends MultiNodeSpec(ClusterNodeProvisionerConfig) with STMultiNodeSpec
with ImplicitSender with ScalaFutures {

  import ClusterNodeProvisionerConfig._

  override def initialParticipants = roles.size

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  val workers = 2
  val idleDetectionPropagationDelayInMilliseconds = 500

  var graph: Graph[Any, Any] = _

  "Signal-Collect" must {
    "start provisioner node" in within(100.seconds){
      runOn(provisioner) {
        assert(!provisionerChildConfig.toString.contains("SignalCollect"))
        graph = GraphBuilder.withActorSystem(system).withNodeProvisioner(new ClusterNodeProvisioner(numberOfNodes = workers,
          config = provisionerChildConfig)).build
      }
      enterBarrier("provisioner up")

      runOn(node1) {
        ClusterNodeEntryPointTemplate.startWithActorSystemName(clusterName, workerNodeConfig)
      }
      enterBarrier("node1 up")

      runOn(provisioner) {
        println("--- asserting")
        try {
          val stats = graph.execute
          assert(stats.individualWorkerStatistics.length == 4 * workers)
        } finally {
          graph.shutdown
        }
      }
      enterBarrier("all done")
    }
  }
}
