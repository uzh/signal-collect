package com.signalcollect

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import org.scalatest.prop.Checkers

import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex

class SerializationIntegrationSpec extends FlatSpec with ShouldMatchers with Checkers with EasyMockSugar {

  "Kryo serialization" should "support running PageRank with message serialization" in {
    val graph = GraphBuilder.
      withMessageSerialization(true).
      build
    try {
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph.addEdge(2, new PageRankEdge(3))
      graph.addEdge(3, new PageRankEdge(2))
      val stats = graph.execute
    } finally {
      graph.shutdown
    }
  }

}
