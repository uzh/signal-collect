package com.signalcollect

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.signalcollect._
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge

class NonExistentVertexHandlerSpec extends FlatSpec with Matchers {

  "Handler for adding an edge to a nonexistent vertex" should "correctly create vertices if needed" in {
    val g = GraphBuilder.build
    try {
      g.setEdgeAddedToNonExistentVertexHandler(TestHandler.createPageRankVertexHandler)
      g.addEdge(1, new PageRankEdge(2))
      g.addEdge(2, new PageRankEdge(1))
      val stats = g.execute
      assert(stats.aggregatedWorkerStatistics.numberOfVertices == 2)
    } finally {
      g.shutdown
    }
  }

}

object TestHandler {
  def createPageRankVertexHandler(edge: Edge[Any], vertexId: Any): Option[Vertex[Any, _]] = {
    val v = new PageRankVertex(vertexId)
    Some(v)
  }
}


