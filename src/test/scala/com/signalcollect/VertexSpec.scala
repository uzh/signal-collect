package com.signalcollect

import language.higherKinds
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.prop.Checkers
import org.scalatest.mock.EasyMockSugar
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.interfaces.SignalMessageWithSourceId
import com.signalcollect.util.TestAnnouncements

class VertexSpec extends FlatSpec with ShouldMatchers with Checkers with EasyMockSugar with TestAnnouncements {

  lazy val smallInt = Gen.chooseNum(0, 100)
  lazy val smallDouble = Gen.chooseNum(0.0, 10.0)

  lazy val signalMapEntry = for {
    k <- smallInt
    v <- smallDouble
  } yield (k, v)

  lazy val signalMap = containerOf[Map, Int, Double](signalMapEntry)

  lazy val outEdgeIds = containerOf[Set, Int](smallInt)

  implicit def arbSignalMap[Map[Int, Double]] = Arbitrary(signalMap)

  implicit def arbEdgeIds[Set[Int]] = Arbitrary(outEdgeIds)

  "PageRankVertex" should "correctly collect and signal" in {
    check(
      (incomingSignals: Map[Int, Double], outgoingEdges: Set[Int]) => {
        val id = "test"
        val mockGraphEditor = mock[GraphEditor[Any, Any]]
        val v = new PageRankVertex(id)
        for (id <- outgoingEdges) {
          v.addEdge(new PageRankEdge(id), mockGraphEditor)
        }
        v.afterInitialization(mockGraphEditor)
        for ((sourceId, signal) <- incomingSignals) {
          v.deliverSignalWithSourceId(signal, sourceId, mockGraphEditor)
        }
        if (!incomingSignals.isEmpty) {
          assert(v.scoreCollect > 0, "vertex received messages, should want to collect")
          v.executeCollectOperation(mockGraphEditor)
          v.state should equal(0.15 + 0.85 * incomingSignals.values.sum +- 0.0001)
          if (!outgoingEdges.isEmpty) {
            assert(v.scoreSignal > 0, "vertex updated state, should want to signal")
            expecting {
              for (targetId <- outgoingEdges) {
                call(mockGraphEditor.sendToWorkerForVertexIdHash(
                  SignalMessageWithSourceId(targetId, id, v.state / outgoingEdges.size), targetId.hashCode))
              }
            }
            whenExecuting(mockGraphEditor) {
              v.executeSignalOperation(mockGraphEditor)
            }
          }
        }
        true
      }, minSuccessful(1000))
  }

}
