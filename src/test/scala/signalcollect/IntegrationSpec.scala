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
import signalcollect.interfaces.Worker._
import signalcollect.algorithms._
import signalcollect.implementations.messaging.MultiQueue
import signalcollect.implementations.worker.DirectDeliveryAsynchronousWorker

/**
 * Hint: For information on how to run specs see the specs v.1 website
 * http://code.google.com/p/specs/wiki/RunningSpecs
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends SpecificationWithJUnit {

  val computeGraphFactories: List[Int => ComputeGraph] = List(
    { workers: Int => new SynchronousComputeGraph(workers) },
    { workers: Int => new SynchronousComputeGraph(workers, workerFactory = Worker.synchronousDirectDeliveryWorkerFactory) },
    { workers: Int => new AsynchronousComputeGraph(workers) },
    { workers: Int => new AsynchronousComputeGraph(workers, workerFactory = Worker.asynchronousDirectDeliveryWorkerFactory) },
    { workers: Int => new AsynchronousComputeGraph(workers, workerFactory = Worker.asynchronousPriorityWorkerFactory) })

  /**
   * Utility Methods
   */
  def buildPageRankGraph(cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        cg.addVertex[Page](sourceId, 0.85)
        cg.addVertex[Page](targetId, 0.85)
        cg.addEdge[Link](sourceId, targetId)
    }
    cg
  }

  def test(graphProviders: List[Int => ComputeGraph], verify: Vertex[_, _] => Boolean, numberOfWorkers: Traversable[Int] = List(8), signalThreshold: Double = 0.001, collectThreshold: Double = 0): Boolean = {
    var correct = true
    var computationStatistics = Map[String, List[ComputationStatistics]]()
    for (workers <- numberOfWorkers) {
      for (graphProvider <- graphProviders) {
        val cg = graphProvider.apply(workers)
        //print("\tMode: " + cg)
        cg.setSignalThreshold(signalThreshold)
        cg.setCollectThreshold(collectThreshold)
        cg.execute
        cg foreach (vertex => if (!verify(vertex)) {
          System.err.println("Test Failed in mode: " + cg + " at vertex: " + vertex.id + " (state:" + vertex.state + ")")
          correct = false
        })
        cg.shutDown
      }
    }
    correct
  }

  def buildVerifiedVertexColoringGraph(numColors: Int, cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        cg.addVertex[VerifiedColoredVertex](sourceId.asInstanceOf[AnyRef], numColors)
        cg.addVertex[VerifiedColoredVertex](targetId.asInstanceOf[AnyRef], numColors)
        cg.addEdge[StateForwarderEdge](sourceId, targetId)
    }
    cg
  }

  def buildSsspGraph(pathSourceId: Any, cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        if (sourceId.equals(pathSourceId)) {
          cg.addVertex[Location](sourceId.asInstanceOf[AnyRef], Some(0))
        } else {
          cg.addVertex[Location](sourceId.asInstanceOf[AnyRef], None)
        }
        if (targetId.equals(pathSourceId)) {
          cg.addVertex[Location](targetId.asInstanceOf[AnyRef], Some(0))
        } else {
          cg.addVertex[Location](targetId.asInstanceOf[AnyRef], None)
        }
        cg.addEdge[Path](sourceId, targetId)
    }
    cg
  }

  "PageRank Algorithm" should {
    "deliver correct results on a 5-cycle graph" in {
      val et1 = List((0, 1), (1, 2), (2, 3), (3, 4), (4, 0))
      val gp1: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildPageRankGraph(cgFactory(workers), et1) }
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        ((v.state.asInstanceOf[Double] - 1).abs < 0.00001)
      }
      test(graphProviders = gp1, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }

    "deliver correct results  a 5-star graph" in {
      val et2 = List((0, 4), (1, 4), (2, 4), (3, 4))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        if (v.id != 4) {
          ((v.state.asInstanceOf[Double] - 0.15).abs < 0.00001)
        } else {
          ((v.state.asInstanceOf[Double] - 0.66).abs < 0.00001)
        }
      }
      val gp2: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildPageRankGraph(cgFactory(workers), et2) }
      test(graphProviders = gp2, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }

    "deliver correct results  a 2*2 symmetric grid" in {
      val et3 = new Grid(2, 2)
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        ((v.state.asInstanceOf[Double] - 1).abs < 0.00001)
      }
      val gp3: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildPageRankGraph(cgFactory(workers), et3) }
      test(graphProviders = gp3, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
  }

  "VertexColoring" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val et1 = List((0, 1), (1, 0), (1, 2), (2, 1), (2, 3), (3, 2), (3, 0), (0, 3))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => false
        }
      }
      val gp1: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildVerifiedVertexColoringGraph(2, cgFactory(workers), et1) }
      test(graphProviders = gp1, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
    "deliver correct results on a symmetric 5-star" in {
      val et2 = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => false
        }
      }
      val gp2: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildVerifiedVertexColoringGraph(2, cgFactory(workers), et2) }
      test(graphProviders = gp2, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
    "deliver correct results on a 2*2 symmetric grid" in {
      val et3 = new Grid(2, 2)
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        v match {
          case c: VerifiedColoredVertex => !c.publicMostRecentSignals.iterator.contains(c.state)
          case other => false
        }
      }
      val gp3: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildVerifiedVertexColoringGraph(2, cgFactory(workers), et3) }
      test(graphProviders = gp3, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
  }

  "SSSP" should {
    "deliver correct results on a symmetric 4-cycle" in {
      val et1 = List((0, 1), (1, 2), (2, 3), (3, 0))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        (v.id == v.state.asInstanceOf[Option[Int]].get)
      }
      val gp1: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildSsspGraph(0, cgFactory(workers), et1) }
      test(graphProviders = gp1, verify _, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
    
    "deliver correct results on a symmetric 5-star" in {
      val et2 = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        if (v.id.asInstanceOf[Int] == 4) {
          v.state.asInstanceOf[Option[Int]].get == 0
        } else {
          v.state.asInstanceOf[Option[Int]].get == 1
        }
      }
      val gp2: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildSsspGraph(4, cgFactory(workers), et2) }
      test(graphProviders = gp2, verify, numberOfWorkers = List(1, 2, 16, 64), signalThreshold = 0, collectThreshold = 0) must_== true
    }
    
    "deliver correct results on a 2*2 symmetric grid" in {
      val et2 = List((0, 4), (4, 0), (1, 4), (4, 1), (2, 4), (4, 2), (3, 4), (4, 3))
      def verify(v: interfaces.Vertex[_, _]): Boolean = {
        if (v.id.asInstanceOf[Int] == 4) {
          v.state.asInstanceOf[Option[Int]].get == 0
        } else {
          v.state.asInstanceOf[Option[Int]].get == 1
        }
      }
      val gp2: List[Int => ComputeGraph] = for (cgFactory <- computeGraphFactories) yield { workers: Int => buildSsspGraph(4, cgFactory(workers), et2) }
      test(graphProviders = gp2, verify, numberOfWorkers = List(1, 2, 4, 8), signalThreshold = 0, collectThreshold = 0)
    }
  }
}

class VerifiedColoredVertex(id: Int, numColors: Int) extends ColoredVertex(id, numColors, 0, false) {
  // only necessary to allow access to vertex internals
  def publicMostRecentSignals: Iterable[Int] = mostRecentSignals
}