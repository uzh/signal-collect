/*
 *  @author Bharath Kumar
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

package com.signalcollect.features

import akka.actor.{ActorRef, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.signalcollect.{MultiJvmConfig, TestConfig, STMultiNodeSpec}
import com.signalcollect.nodeprovisioning.cluster.{ClusterNodeProvisionerActor, RetrieveNodeActors}
import com.signalcollect.util.TestAnnouncements
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._
import scala.language.postfixOps

class ClusterSpecMultiJvmNode1 extends ClusterSpec

//class ClusterSpecMultiJvmNode2 extends ClusterSpec

//class ClusterSpecMultiJvmNode3 extends ClusterSpec

object ClusterSpecConfig extends MultiNodeConfig {
  val provisioner = role("provisioner")
//  val worker1 = role("worker1")
//  val worker2 = role("worker2")

  val clusterName = "ClusterSpec"
  val seedPort = 2557

  nodeConfig(provisioner) {
    MultiJvmConfig.provisionerCommonConfig(seedPort)
  }

  // this configuration will be used for all nodes
  commonConfig {
    MultiJvmConfig.nodeCommonConfig(clusterName, seedPort)
  }
}

class ClusterSpec extends MultiNodeSpec(ClusterSpecConfig) with STMultiNodeSpec
with ImplicitSender with TestAnnouncements with ScalaFutures {

  import ClusterSpecConfig._

  override def initialParticipants = roles.size

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(1000, Millis)))

  val workers = roles.size
  val prefix = TestConfig.prefix

  muteDeadLetters(classOf[Any])(system)

  "SignalCollect" should {
    "get the cluster up" in {
      runOn(provisioner) {
        system.actorOf(Props(classOf[ClusterNodeProvisionerActor], MultiJvmConfig.idleDetectionPropagationDelayInMilliseconds,
          prefix, workers), "ClusterMasterBootstrap")
      }
      enterBarrier("all nodes up")

      runOn(provisioner) {
        implicit val timeout = Timeout(300.seconds)
        val masterActor = system.actorSelection(node(provisioner) / "user" / "ClusterMasterBootstrap")
        val nodeActorsFuture = (masterActor ? RetrieveNodeActors).mapTo[Array[ActorRef]]
        whenReady(nodeActorsFuture) { nodeActors =>
          assert(nodeActors.length == workers)
        }
      }
      enterBarrier("tests done!")
    }
  }
}
