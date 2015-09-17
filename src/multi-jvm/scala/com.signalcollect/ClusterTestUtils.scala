package com.signalcollect

import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.interfaces.ModularAggregationOperation

/**
 * Created by bharath on 17/09/15.
 */
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
