/*
 *  @author Thomas Keller
 *
 *  Copyright 2013 University of Zurich
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

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GraphResetSpec extends SpecificationWithJUnit {

  sequential

  "Graph.reset" should {

    "reset the graph into a state similar to the one it had after Graph.build" in {

      val editGraph = (graph: Graph[Any, Any]) => {
        graph.addVertex(new PageRankVertex(1))
        graph.addVertex(new PageRankVertex(2))
        graph.addVertex(new PageRankVertex(3))
        graph.addEdge(1, new PageRankEdge(2))
        graph.addEdge(2, new PageRankEdge(1))
        graph.addEdge(2, new PageRankEdge(3))
        graph.addEdge(3, new PageRankEdge(2))
      }

      val graph = GraphBuilder.build
      graph.awaitIdle
      editGraph(graph)
      graph.awaitIdle
      val firstEI = graph.execute // First execution (should in many aspects be similar to the third execution)
      graph.awaitIdle
      graph.reset // Reset
      graph.awaitIdle
      val secondEI = graph.execute // Second execution in reseted state (should be different from the other two executions)
      graph.awaitIdle
      graph.reset // Reset
      graph.awaitIdle
      editGraph(graph)
      graph.awaitIdle
      val thirdEI = graph.execute // Third execution (should in many aspects be similar to the first execution)
      graph.awaitIdle
      graph.shutdown

      // Comparisons

      // Execution Information
      // Do not compare EI.config
      // Do not compare EI.numberOfWorkers
      // Do not compare EI.nodesDescriptions
      // Do not compare EI.parameters

      // Execution Statistics
      val firstES = firstEI.executionStatistics
      val secondES = secondEI.executionStatistics
      val thirdES = thirdEI.executionStatistics
      firstES.signalSteps === secondES.signalSteps
      secondES.signalSteps === thirdES.signalSteps
      firstES.collectSteps === secondES.collectSteps
      secondES.collectSteps === thirdES.collectSteps
      // Do not compare ES.computationTime
      // Do not compare ES.totalExecutionTime
      // Do not compare ES.jvmCpuTime
      // Do not compare ES.graphIdleWaitingTime
      // Do not compare ES.terminationReason

      // Aggregated Worker Statistics
      val firstAWS = firstEI.aggregatedWorkerStatistics
      val secondAWS = secondEI.aggregatedWorkerStatistics
      val thirdAWS = thirdEI.aggregatedWorkerStatistics
      // Do not compare AWS.messagesSent
      // Do not compare AWS.workerId
      // Do not compare AWS.messagesReceived
      // Do not compare AWS.toSignalSize
      // Do not compare AWS.toCollectSize
      // Do not compare collectOperationsExecuted between firstAWS and thirdAWS, but:
      secondAWS.collectOperationsExecuted === 0
      // Do not compare signalOperationsExecuted between firstAWS and thirdAWS, but:
      secondAWS.signalOperationsExecuted === 0
      firstAWS.numberOfVertices === thirdAWS.numberOfVertices
      secondAWS.numberOfVertices === 0
      firstAWS.verticesAdded === thirdAWS.verticesAdded
      secondAWS.verticesAdded === 0
      firstAWS.verticesRemoved === thirdAWS.verticesRemoved
      secondAWS.verticesRemoved === 0
      firstAWS.numberOfOutgoingEdges === thirdAWS.numberOfOutgoingEdges
      secondAWS.numberOfOutgoingEdges === 0
      firstAWS.outgoingEdgesAdded === thirdAWS.outgoingEdgesAdded
      secondAWS.outgoingEdgesAdded === 0
      firstAWS.outgoingEdgesRemoved === thirdAWS.outgoingEdgesRemoved
      secondAWS.outgoingEdgesRemoved === 0
      // Do not compare AWS.receiveTimeoutMessagesReceived
      // Do not compare AWS.heartbeatMessagesReceived
      // Do not compare AWS.signalMessagesReceived
      // Do not compare AWS.bulkSignalMessagesReceived
      // Do not compare AWS.continueMessagesReceived
      // Do not compare AWS.requestMessagesReceived
      // Do not compare AWS.otherMessagesReceived

      // Do not compare the individual worker statistics
    }
  }
}
