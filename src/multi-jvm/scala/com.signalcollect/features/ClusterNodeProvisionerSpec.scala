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
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import com.signalcollect.GraphBuilder
import com.signalcollect.nodeprovisioning.cluster.ClusterNodeProvisioner
import com.signalcollect.util.TestAnnouncements
import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

trait STMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with ShouldMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}

class ClusterNodeProvisionerSpecMultiJvmNode1 extends ClusterNodeProvisionerSpec

class ClusterNodeProvisionerSpecMultiJvmNode2 extends ClusterNodeProvisionerSpec

class ClusterNodeProvisionerSpecMultiJvmNode3 extends ClusterNodeProvisionerSpec

object MultiNodeTestConfig extends MultiNodeConfig {
  val master = role("master")
  val worker1 = role("worker1")
  val worker2 = role("worker2")

  // this configuration will be used for all nodes
  // note that no fixed host names and ports are used
  commonConfig(ConfigFactory.load())
}

class ClusterNodeProvisionerSpec extends MultiNodeSpec(MultiNodeTestConfig) with STMultiNodeSpec
with ImplicitSender with TestAnnouncements {

  import MultiNodeTestConfig._

  override def initialParticipants = roles.size

  val masterAddress = node(master).address
  val worker1Address = node(worker1).address
  val worker2Address = node(worker2).address
  val workers = 2

  muteDeadLetters(classOf[Any])(system)

  "SignalCollect" should {

    "form the cluster" in within(300 seconds) {
      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])

      Cluster(system).join(masterAddress)
      receiveN(3).map { case MemberUp(m) => m.address }.toSet should be(Set(masterAddress, worker1Address, worker2Address))
      Cluster(system).unsubscribe(testActor)
      testConductor.enter("cluster-up")
    }

    "support setting the number of workers created on each node" in {
      runOn(master) {
        val graph = GraphBuilder.withNodeProvisioner(new ClusterNodeProvisioner(numberOfNodes = workers)).build
        try {
          val stats = graph.execute
          stats.individualWorkerStatistics.length == workers
        } finally {
          graph.shutdown
        }
      }
      testConductor.enter("finished")
    }
  }

  override def beforeAll: Unit = multiNodeSpecBeforeAll()

  override def afterAll: Unit = multiNodeSpecAfterAll()
}