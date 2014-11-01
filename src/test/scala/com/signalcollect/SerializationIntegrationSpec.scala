package com.signalcollect

import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import org.scalatest.prop.Checkers
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.examples.PlaceholderEdge
import com.signalcollect.util.TestAnnouncements

class SerializationIntegrationSpec extends FlatSpec with ShouldMatchers with Checkers with EasyMockSugar with TestAnnouncements {

  "Kryo serialization" should "support running PageRank with message serialization" in {
    val graph = new GraphBuilder[Int, Double]().
      withMessageSerialization(true).
      build
    try {
      graph.addVertex(new EfficientPageRankVertex(1))
      graph.addVertex(new EfficientPageRankVertex(2))
      graph.addVertex(new EfficientPageRankVertex(3))
      graph.addEdge(1, new PlaceholderEdge(2))
      graph.addEdge(2, new PlaceholderEdge(1))
      graph.addEdge(2, new PlaceholderEdge(3))
      graph.addEdge(3, new PlaceholderEdge(2))
      val stats = graph.execute
    } finally {
      graph.shutdown
    }
  }

}
