/*
 *  @author Philip Stutz
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

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.signalcollect.examples.Location
import com.signalcollect.examples.Path
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.util.TestAnnouncements
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import com.signalcollect.distributed.DistributedSimulator

class DelayingVertex(id: Int, initialState: Int = 0) extends DataGraphVertex(id, initialState) {
  def collect = {
    Thread.sleep(500)
    id
  }
}

class WorkerApiSpec extends FlatSpec with ShouldMatchers with TestAnnouncements {

  "Worker API" should "not block on a subset of workers" in {
    val startTime = System.currentTimeMillis
    val kryo = List("com.signalcollect.DelayingVertex", "com.signalcollect.StateForwarderEdge")
    val nodes = 4
    val workersPerNode = 4
    val numberOfWorkers = nodes * workersPerNode
    val nodeActors = DistributedSimulator.getNodeActors(
        nodes, workersPerNode, 1,
        kryo)
    val g = GraphBuilder.
      withPreallocatedNodes(nodeActors).
      build
    try {
      g.awaitIdle
      for (id <- 0 until numberOfWorkers) {
        val v = new DelayingVertex(id, -1)
        g.addVertex(v)
        val nextId = (id + 1) % numberOfWorkers
        g.addEdge(id, new StateForwarderEdge(nextId))
      }
      g.awaitIdle
      println(g.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous)))
    } finally {
      g.shutdown
    }
    val stopTime = System.currentTimeMillis
    val t = stopTime - startTime
    //assert(t < 2000, s"Execution took $t milliseconds, should be less than 2 seconds.")
  }

}
