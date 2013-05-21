/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
 *
 *  Copyright 2012 University of Zurich
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
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GraphModificationSpec extends SpecificationWithJUnit {

  sequential

  "GraphEditor" should {
    "support modification functions" in {
      val graph = GraphBuilder.build
      graph.modifyGraph({ _.addVertex(new GraphModificationVertex(0, 1)) }, Some(0))
      graph.modifyGraph({ _.addVertex(new GraphModificationVertex(1, 1)) }, Some(1))
      graph.modifyGraph({ _.addVertex(new GraphModificationVertex(2, 1)) }, Some(2))
      graph.modifyGraph({ _.addVertex(new GraphModificationVertex(3, 1)) }, Some(3))
      graph.modifyGraph({ _.addEdge(0, new StateForwarderEdge(1)) }, Some(0))
      graph.modifyGraph({ _.addEdge(1, new StateForwarderEdge(3)) }, Some(1))
      var statistics = graph.execute
      graph.aggregate(new CountVertices[GraphModificationVertex]) === 4
      statistics.aggregatedWorkerStatistics.numberOfVertices === 4
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 0
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 0
      graph.modifyGraph({ _.removeVertex(0, true) }, Some(0))
      graph.modifyGraph({ _.removeVertex(2, true) }, Some(2))
      statistics = graph.execute
      graph.aggregate(new CountVertices[GraphModificationVertex]) === 2
      graph.shutdown
      statistics.aggregatedWorkerStatistics.numberOfVertices === 2
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 2
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 1
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 1
    }

    "keep accurate statistics when using individual vertex removals" in {
      val graph = GraphBuilder.build
      graph.addVertex(new GraphModificationVertex(0, 1))
      graph.addVertex(new GraphModificationVertex(1, 1))
      graph.addVertex(new GraphModificationVertex(2, 1))
      graph.addVertex(new GraphModificationVertex(3, 1))
      graph.addEdge(0, new StateForwarderEdge(1))
      graph.addEdge(1, new StateForwarderEdge(3))
      var statistics = graph.execute
      graph.aggregate(new CountVertices[GraphModificationVertex]) === 4
      statistics.aggregatedWorkerStatistics.numberOfVertices === 4
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 0
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 0
      graph.removeVertex(0, true)
      graph.removeVertex(2, true)
      statistics = graph.execute
      graph.aggregate(new CountVertices[GraphModificationVertex]) === 2
      graph.shutdown
      statistics.aggregatedWorkerStatistics.numberOfVertices === 2
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 2
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 1
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 1
    }
  }
}

class GraphModificationVertex(id: Int, state: Int) extends DataGraphVertex(id, state) {
  def collect = 1
}