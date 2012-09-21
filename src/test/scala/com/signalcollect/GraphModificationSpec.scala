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

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.signalcollect._

@RunWith(classOf[JUnitRunner])
class GraphModificationSpec extends SpecificationWithJUnit {

  sequential
  
  "GraphEditor" should {

    "keep accurate statistics when using predicate-based vertex removals" in {
      val g = GraphBuilder.build
      g.addVertex(new GraphModificationVertex(0, 1))
      g.addVertex(new GraphModificationVertex(1, 1))
      g.addVertex(new GraphModificationVertex(2, 1))
      g.addVertex(new GraphModificationVertex(3, 1))
      g.addEdge(0, new StateForwarderEdge(1))
      g.addEdge(1, new StateForwarderEdge(3))
      var statistics = g.execute
      g.aggregate(new CountVertices[GraphModificationVertex]) === 4
      statistics.aggregatedWorkerStatistics.numberOfVertices === 4
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 0
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 0
      g.removeVertices(v => (v.asInstanceOf[GraphModificationVertex].id % 2 == 0))
      statistics = g.execute
      g.aggregate(new CountVertices[GraphModificationVertex]) === 2
      statistics.aggregatedWorkerStatistics.numberOfVertices === 2
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 2
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 1
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 1
    }
    
    "keep accurate statistics when using individual vertex removals" in {
      val g = GraphBuilder.build
      g.addVertex(new GraphModificationVertex(0, 1))
      g.addVertex(new GraphModificationVertex(1, 1))
      g.addVertex(new GraphModificationVertex(2, 1))
      g.addVertex(new GraphModificationVertex(3, 1))
      g.addEdge(0, new StateForwarderEdge(1))
      g.addEdge(1, new StateForwarderEdge(3))
      var statistics = g.execute
      g.aggregate(new CountVertices[GraphModificationVertex]) === 4
      statistics.aggregatedWorkerStatistics.numberOfVertices === 4
      statistics.aggregatedWorkerStatistics.verticesAdded === 4
      statistics.aggregatedWorkerStatistics.verticesRemoved === 0
      statistics.aggregatedWorkerStatistics.numberOfOutgoingEdges === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesAdded === 2
      statistics.aggregatedWorkerStatistics.outgoingEdgesRemoved === 0
      g.removeVertex(0, true)
      g.removeVertex(2, true)
      statistics = g.execute
      g.aggregate(new CountVertices[GraphModificationVertex]) === 2
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
  type Signal = Int
  def collect(oldState: Int, mostRecentSignals: Iterable[Int], graphEditor: GraphEditor): Int = {
    1
  }
}