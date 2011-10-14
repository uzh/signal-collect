/*
 *  @author Philip Stutz
 *  
 *  Copyright 2011 University of Zurich
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

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import java.util.Map.Entry
import com.signalcollect._
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.SudokuCell

@RunWith(classOf[JUnitRunner])
class IncomingEdgeCountSpec extends SpecificationWithJUnit with Mockito {

  "incoming edge count" should {
    val graph = GraphBuilder.build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    graph.addEdge(new PageRankEdge(1, 2))
    graph.addEdge(new PageRankEdge(2, 1))
    graph.execute(ExecutionConfiguration.withSignalThreshold(0))

    "count one edge correctly" in {
      val incomingEdgeCount2 = graph.forVertexWithId(2, (v: PageRankVertex) => v.incomingEdgeCount).get
      incomingEdgeCount2 == 1
    }

    "count two edges correctly" in {
      graph.addEdge(new PageRankEdge(3, 2))
      val incomingEdgeCount2 = graph.forVertexWithId(2, (v: PageRankVertex) => v.incomingEdgeCount).get
      incomingEdgeCount2 == 2
    }

    "handle edge removals correctly" in {
      graph.removeEdge(DefaultEdgeId(1, 2, ""))
      val incomingEdgeCount2 = graph.forVertexWithId(2, (v: PageRankVertex) => v.incomingEdgeCount).get
      incomingEdgeCount2 == 1
    }
  }

}