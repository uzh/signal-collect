/*
 *  @author Carol Alexandru
 *
 *  Copyright 2015 University of Zurich
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

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.signalcollect.util.TestAnnouncements
import com.signalcollect._
import com.signalcollect.util.TestAnnouncements
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex

class MultipleGraphsSpec extends FlatSpec with Matchers with TestAnnouncements {

  def createComputation(p: String, port: Int): Graph[_, _] = {
    val system = TestConfig.actorSystem(port = port)
    val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(p).build
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addEdge(1, new PageRankEdge(2))
    graph.addEdge(2, new PageRankEdge(1))
    graph
  }

  "Signal/Collect" should "support running multiple graph instances on the same actor system" in {
    val graph1 = createComputation("prefix1", 2556)
    val graph2 = createComputation("prefix2", 2557)
    val graph3 = createComputation("prefix3", 2558)
    graph1.execute
    graph2.execute
    graph3.execute
    graph1.awaitIdle
    graph2.awaitIdle
    graph3.awaitIdle
    graph1.shutdown
    graph1.system.shutdown
    graph2.shutdown
    graph2.system.shutdown
    graph3.shutdown
    graph3.system.shutdown
  }

}
