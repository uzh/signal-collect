/*
 *  @author Bharath Kumar
 *
 *  Copyright 2015 iHealth Technologies
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

import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.interfaces.ModularAggregationOperation

object ClusterTestUtils {
  val executionModes = List(ExecutionMode.ContinuousAsynchronous)

  def test(graphProviders: List[() => Graph[Any, Any]], verify: Vertex[_, _, _, _] => Boolean, buildGraph: Graph[Any, Any] => Unit = (graph: Graph[Any, Any]) => (), signalThreshold: Double = 0.01, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ExecutionInformation[Any, Any]]]()

    for (executionMode <- executionModes) {
      for (graphProvider <- graphProviders) {
        val graph = graphProvider()
        try {
          buildGraph(graph)
          graph.awaitIdle
          val stats = graph.execute(ExecutionConfiguration(executionMode = executionMode, signalThreshold = signalThreshold))
          graph.awaitIdle
          correct &= graph.aggregate(new ModularAggregator(verify))
          if (!correct) {
            System.err.println("Test failed. Computation stats: " + stats)
          }
        } finally {
          graph.shutdown
        }
      }
    }
    correct
  }
}

class ModularAggregator(verify: Vertex[_, _, _, _] => Boolean) extends ModularAggregationOperation[Boolean] {
  val neutralElement = true

  def aggregate(a: Boolean, b: Boolean): Boolean = a && b

  def extract(v: Vertex[_, _, _, _]): Boolean = verify(v)
}
