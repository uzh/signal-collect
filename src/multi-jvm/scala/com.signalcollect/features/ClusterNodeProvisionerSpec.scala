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

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import com.signalcollect.GraphBuilder
import com.signalcollect.nodeprovisioning.cluster.ClusterNodeProvisioner
import com.signalcollect.util.TestAnnouncements
import org.scalatest._

trait STMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}

object MultiNodeTestConfig extends MultiNodeConfig {
  val node1 = role("node1")
}

class ClusterNodeProvisionerSpecMultiJvmNode1 extends MultiNodeSpec(MultiNodeTestConfig) with STMultiNodeSpec
with ImplicitSender with TestAnnouncements {

  import MultiNodeTestConfig._

  override def initialParticipants = roles.size

  "Clustered Signal/Collect" should {

    "wait for all nodes to enter a barrier" in {
      testConductor.enter("startup")
    }

    "support setting the number of workers created on each node" in {
      runOn(node1) {
        testConductor.enter("all deployed")
        val workers = 2
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