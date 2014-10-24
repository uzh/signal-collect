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

package com.signalcollect.distributed

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.signalcollect.examples.Location
import com.signalcollect.examples.Path
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.AkkaConfig
import akka.event.Logging
import java.net.InetAddress
import akka.actor.ActorSystem
import akka.actor.Props
import com.signalcollect.node.DefaultNodeActor
import com.signalcollect.TestAnnouncements
import com.signalcollect.GraphBuilder
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.ActorRef
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.SumOfStates
import com.signalcollect.SumOfStates
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory

object DistributedSimulator {
  def getNodeActors(numberOfSimulatedNodes: Int, workersPerSimulatedNode: Int): Array[ActorRef] = {
    val totalNumberOfWorkers = numberOfSimulatedNodes * workersPerSimulatedNode
    val akkaConfig = AkkaConfig.get(serializeMessages = false,
      loggingLevel = Logging.WarningLevel,
      kryoRegistrations = List("com.signalcollect.Graph$$anon$1"),
      kryoInitializer = "com.signalcollect.configuration.KryoInit",
      hostname = InetAddress.getLocalHost.getHostAddress,
      port = 0,
      numberOfCores = workersPerSimulatedNode)
    val actorSystems = (0 until numberOfSimulatedNodes).map {
      systemId => (systemId, ActorSystem(if (systemId == 0) "SignalCollect" else s"SignalCollect$systemId", akkaConfig))
    }
    val nodeActors = actorSystems.map {
      case (systemId, system) =>
        system.actorOf(
          Props(classOf[DefaultNodeActor[Any, Any]], "", systemId, numberOfSimulatedNodes, Some(workersPerSimulatedNode), None),
          name = s"DefaultNodeActor$systemId")
    }
    ActorSystemRegistry.register(actorSystems(0)._2)
    nodeActors.toArray
  }
}

class DistributedSimulationSpec extends FlatSpec with ShouldMatchers with TestAnnouncements {

  "Signal/Collect" should "terminate with a low latency when run in a simulated distributed synchronous mode" in {
    val numberOfSimulatedNodes = 5
    val workersPerSimulatedNode = 2
    val circleLength = 10
    val nodeActors = DistributedSimulator.getNodeActors(numberOfSimulatedNodes, workersPerSimulatedNode)
    val startTime = System.currentTimeMillis
    val g = GraphBuilder.
      withMessageBusFactory(new BulkAkkaMessageBusFactory[Any, Any](10000, true)).
      withPreallocatedNodes(nodeActors).
      withStatsReportingInterval(0).
      build
    try {
      (1 to 10).foreach { i =>
        g.awaitIdle
        for (i <- 0 until circleLength) {
          g.addVertex(new PageRankVertex(i))
          g.addEdge(i, new PageRankEdge((i + 1) % circleLength))
        }
        g.awaitIdle
        println(g.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous).withSignalThreshold(0)))
        //println(g.execute(ExecutionConfiguration.withSignalThreshold(0)))
        val stateSum = g.aggregate(SumOfStates[Double])
        stateSum === circleLength.toDouble +- 0.00001
        g.reset
      }
    } finally {
      g.shutdown
    }
    val stopTime = System.currentTimeMillis
    val t = stopTime - startTime
    assert(t < 30000, s"Execution took $t milliseconds, should be less than 30 seconds.")
  }

}
