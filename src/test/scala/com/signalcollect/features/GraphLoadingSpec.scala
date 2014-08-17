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
 *
 */

package com.signalcollect.features

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.CountVertices
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GraphBuilder
import com.signalcollect.ProductOfStates
import com.signalcollect.SampleVertexIds
import com.signalcollect.SumOfStates
import com.signalcollect.TopKFinder
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.SudokuCell
import org.specs2.runner.JUnitRunner
import com.signalcollect.GraphEditor

@RunWith(classOf[JUnitRunner])
class GraphLoadingSpec extends SpecificationWithJUnit {

  sequential

  "Graph" should {

    "support the loadGraph command" in {
      val graph = GraphBuilder.build
      try {
        val graphLoaders = (1 to 100).map(x => (10 * x until ((10 * x) + 10)).toIterator.map(y => new PageRankVertex(y)).map(z => {
          ge: GraphEditor[Any, Any] =>
            ge.addVertex(z)
        }))
        for (loader <- graphLoaders) {
          graph.loadGraph(loader, Some(0))
        }
        graph.awaitIdle
        val stats = graph.execute(ExecutionConfiguration.withSignalThreshold(0.01))
        if (stats.aggregatedWorkerStatistics.numberOfVertices != 1000) {
          println(s"Only ${stats.aggregatedWorkerStatistics.numberOfVertices} vertices were added, instead of 1000.")
        }
        stats.aggregatedWorkerStatistics.numberOfVertices == 1000
      } finally {
        graph.shutdown
      }
    }

  }

}
