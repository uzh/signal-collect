/**
 *  @author Philip Stutz
 *  @author Tobias Bachmann
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

package com.signalcollect.deployment

import com.signalcollect.Graph
import com.signalcollect.GraphBuilder
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import akka.event.Logging

/**
 * Simple Example of an Algorithm
 */
object PageRankExample extends Algorithm[Any, Any] {
  
  override def deploy: Boolean = false
  
  override def loadGraph(graph: Graph[Any, Any]): Graph[Any, Any] = {
     log.info("add vertices")
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    log.info("add edges")
    graph.addEdge(1, new PageRankEdge(2))
    graph.addEdge(2, new PageRankEdge(1))
    graph.addEdge(2, new PageRankEdge(3))
    graph.addEdge(3, new PageRankEdge(2))
    graph
  }
  
  override def configureGraphBuilder(gb: GraphBuilder[Any, Any]): GraphBuilder[Any, Any] = {
    gb
    .withEagerIdleDetection(false)
    .withLoggingLevel(Logging.DebugLevel)
  }

}
