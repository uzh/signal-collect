package com.signalcollect

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.signalcollect._
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandler

class TestEdgeAddedToNonExistentVertexHandlerFactory[@specialized(Long) Id, Signal] extends EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal] {
  def createInstance: EdgeAddedToNonExistentVertexHandler[Id, Signal] =
    new TestEdgeAddedToNonExistentVertexHandler[Id, Signal]
  override def toString = "TestEdgeAddedToNonExistentVertexHandlerFactory"
}

class TestEdgeAddedToNonExistentVertexHandler[@specialized(Long) Id, Signal] extends EdgeAddedToNonExistentVertexHandler[Id, Signal] {
  def handleImpossibleEdgeAddition(edge: Edge[Id], vertexId: Id): Option[Vertex[Id, _]] = {
    val v = new PageRankVertex(vertexId)
    Some(v)
  }
}

class NonExistentVertexHandlerSpec extends FlatSpec with Matchers {

  "Handler for adding an edge to a nonexistent vertex" should "correctly create vertices if needed" in {
    val g = GraphBuilder.withEdgeAddedToNonExistentVertexHandlerFactory(new TestEdgeAddedToNonExistentVertexHandlerFactory[Any, Any]).build
    try {
      g.addEdge(1, new PageRankEdge(2))
      g.addEdge(2, new PageRankEdge(1))
      val stats = g.execute
      assert(stats.aggregatedWorkerStatistics.numberOfVertices == 2)
    } finally {
      g.shutdown
    }
  }

}
