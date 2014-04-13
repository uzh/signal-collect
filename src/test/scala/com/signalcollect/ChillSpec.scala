package com.signalcollect

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ChillSpec extends FlatSpec with Matchers {

  "Chill serializer" should "correctly serialize a Scala map" in {
    val g = GraphBuilder.withMessageSerialization(true).build
    try {
      g.addVertex(new WeirdVertex(1, Some(Map(1 -> 2.0))))
      g.addVertex(new WeirdVertex(2, Some(Map(2 -> 4.0))))
      g.addEdge(1, new StateForwarderEdge(2))
      g.addEdge(2, new StateForwarderEdge(1))
      g.execute
    } finally {
      g.shutdown
    }
  }

}

class WeirdVertex(id: Int, initialState: Option[Map[Int, Double]])
  extends DataGraphVertex[Int, Option[Map[Int, Double]]](id, initialState) {
  type Signal = Option[Map[Int, Double]]
  def collect = signals.head
}
