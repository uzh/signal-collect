/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
 *  
 *  Copyright 2011 University of Zurich
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

package signalcollect

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import signalcollect._
import signalcollect.api._
import signalcollect.interfaces._
import signalcollect.graphproviders._
import signalcollect.api.Factory._
import signalcollect.algorithms._
import signalcollect.implementations.worker.DirectDeliveryAsynchronousWorker

/**
 * Hint: For information on how to run specs see the specs v.1 website
 * http://code.google.com/p/specs/wiki/RunningSpecs
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends SpecificationWithJUnit {

  val synchronousCg = DefaultSynchronousBuilder

  val computeGraphFactories: List[Int => ComputeGraph] = List(
    (numberOfWorkers: Int) => DefaultSynchronousBuilder.withNumberOfWorkers(numberOfWorkers).build,
    (numberOfWorkers: Int) => DefaultSynchronousBuilder.withNumberOfWorkers(numberOfWorkers).withWorkerFactory(Factory.Worker.Synchronous).build,
    (numberOfWorkers: Int) => DefaultBuilder.withNumberOfWorkers(numberOfWorkers).build,
    (numberOfWorkers: Int) => DefaultBuilder.withNumberOfWorkers(numberOfWorkers).withWorkerFactory(Factory.Worker.Asynchronous).build,
    (numberOfWorkers: Int) => DefaultBuilder.withNumberOfWorkers(numberOfWorkers).withWorkerFactory(Factory.Worker.AsynchronousPriority).build)

  val testWorkerCounts = List(1, 2, 16, 64)

  def test(graphProviders: List[Int => ComputeGraph] = computeGraphFactories, verify: Vertex[_, _] => Boolean, buildGraph: ComputeGraph => Unit = (cg: ComputeGraph) => (), numberOfWorkers: Traversable[Int] = testWorkerCounts, signalThreshold: Double = 0, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ComputationStatistics]]()
    for (workers <- numberOfWorkers) {
      for (graphProvider <- graphProviders) {
        val cg = graphProvider.apply(workers)
        buildGraph(cg)
        cg.setSignalThreshold(signalThreshold)
        cg.setCollectThreshold(collectThreshold)
        val stats = cg.execute
        correct &= cg.customAggregate(true, (a: Boolean, b: Boolean) => (a && b), verify)
        if (!correct) {
          System.err.println("Test failed. Computation stats: " + stats)
        }
        cg.shutDown
      }
    }
    correct
  }

  def buildPageRankGraph(cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId: Int, targetId: Int) =>
        cg.addVertex(classOf[Page], sourceId, 0.85)
        cg.addVertex(classOf[Page], targetId, 0.85)
        cg.addEdge(classOf[Link], sourceId, targetId)
    }
    cg
  }

  def buildVertexColoringGraph(numColors: Int, cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        cg.addVertex(classOf[VerifiedColoredVertex], sourceId.asInstanceOf[AnyRef], numColors)
        cg.addVertex(classOf[VerifiedColoredVertex], targetId.asInstanceOf[AnyRef], numColors)
        cg.addEdge(classOf[StateForwarderEdge], sourceId, targetId)
    }
    cg
  }

  def buildSsspGraph(pathSourceId: Any, cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        if (sourceId.equals(pathSourceId)) {
          cg.addVertex(classOf[Location], sourceId.asInstanceOf[AnyRef], Some(0))
        } else {
          cg.addVertex(classOf[Location], sourceId.asInstanceOf[AnyRef], None)
        }
        if (targetId.equals(pathSourceId)) {
          cg.addVertex(classOf[Location], targetId.asInstanceOf[AnyRef], Some(0))
        } else {
          cg.addVertex(classOf[Location], targetId.asInstanceOf[AnyRef], None)
        }
        cg.addEdge(classOf[Path], sourceId, targetId)
    }
    cg
  }

  "PageRank algorithm" should {
    "deliver correct results on a 5-cycle graph" in {
      val fiveCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))
      def pageRankFiveCycleVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.00001
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = pageRankFiveCycleVerifier, buildGraph = buildPageRankGraph(_, fiveCycleEdges)) must_== true
    }

    "deliver correct results on a 5-star graph" in {
      val fiveStarEdges = List((0, 4), (1, 4), (2, 4), (3, 4))
      def pageRankFiveStarVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = if (v.id == 4.0) 0.66 else 0.15
        val correct = (state - expectedState).abs < 0.00001
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = pageRankFiveStarVerifier, buildGraph = buildPageRankGraph(_, fiveStarEdges)) must_== true
    }

    "deliver correct results on a 2*2 symmetric grid" in {
      val symmetricTwoOnTwoGridEdges = new Grid(2, 2)
      def pageRankTwoOnTwoGridVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.00001
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = pageRankTwoOnTwoGridVerifier, buildGraph = buildPageRankGraph(_, symmetricTwoOnTwoGridEdges)) must_== true
    }
  }

  "VertexColoring algorithm" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val symmetricFourCycleEdges = List((0, 1), (1, 0), (1, 2), (2, 1), (2, 3), (3, 2), (3, 0), (0, 3))
      def vertexColoringVerifier(v: Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => System.out.println("Problematic vertex:  id=" + v.id + ". Color collides with neighboring vertex."); false
        }
      }
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFourCycleEdges)) must_== true
    }

    "deliver correct results on a symmetric 5-star" in {
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def vertexColoringVerifier(v: Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => System.out.println("Problematic vertex:  id=" + v.id + ". Color collides with neighboring vertex."); false
        }
      }
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricFiveStarEdges)) must_== true
    }
    "deliver correct results on a 2*2 symmetric grid" in {
      val symmetricTwoOnTwoGridEdges = new Grid(2, 2)
      def vertexColoringVerifier(v: Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => System.out.println("Problematic vertex:  id=" + v.id + ". Color collides with neighboring vertex."); false
        }
      }
      test(verify = vertexColoringVerifier, buildGraph = buildVertexColoringGraph(2, _, symmetricTwoOnTwoGridEdges)) must_== true
    }
  }

  "SSSP algorithm" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val symmetricFourCycleEdges = List((0, 1), (1, 2), (2, 3), (3, 0))
      def ssspSymmetricsFourCycleVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Option[Int]].get
        val expectedState = v.id
        val correct = state == expectedState
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = ssspSymmetricsFourCycleVerifier, buildGraph = buildSsspGraph(0, _, symmetricFourCycleEdges)) must_== true
    }

    "deliver correct results on a symmetric 5-star" in {
      val symmetricFiveStarEdges = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def ssspSymmetricFiveStarVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Option[Int]].get
        val expectedState = if (v.id == 4) 0 else 1
        val correct = state == expectedState
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      test(verify = ssspSymmetricFiveStarVerifier, buildGraph = buildSsspGraph(4, _, symmetricFiveStarEdges)) must_== true
    }

  }
}

class VerifiedColoredVertex(id: Int, numColors: Int) extends ColoredVertex(id, numColors, 0, false) {
  // only necessary to allow access to vertex internals
  def publicMostRecentSignals: Iterable[Int] = mostRecentSignals
}