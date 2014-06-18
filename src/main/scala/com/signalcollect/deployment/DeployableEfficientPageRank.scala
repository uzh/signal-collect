/*
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

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.examples.PlaceholderEdge

/** Builds a PageRank compute graph and executes the computation */
class DeployableEfficientPageRank extends DeployableAlgorithm {
  override def execute(parameters: Map[String, String], graphBuilder: GraphBuilder[Any, Any]) {
    val graph = graphBuilder.
      //    withConsole(true).
      build

    graph.awaitIdle
    graph.addVertex(new EfficientPageRankVertex(1))
    graph.addVertex(new EfficientPageRankVertex(2))
    graph.addVertex(new EfficientPageRankVertex(3))
    graph.addEdge(1, new PlaceholderEdge(2))
    graph.addEdge(2, new PlaceholderEdge(1))
    graph.addEdge(2, new PlaceholderEdge(3))
    graph.addEdge(3, new PlaceholderEdge(2))

    graph.awaitIdle
    val stats = graph.execute //(ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive))
    println(stats)

    graph.foreachVertex(println(_))
    graph.shutdown
  }
}
