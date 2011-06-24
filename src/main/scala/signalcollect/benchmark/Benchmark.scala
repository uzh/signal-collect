/*
 *  @author Lorenz Fischer
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

package signalcollect.benchmark

import scala.util.Random
import signalcollect.interfaces.ComputeGraph
import signalcollect.api.DefaultBuilder
import signalcollect.algorithms.Page
import signalcollect.algorithms.Link
import signalcollect.interfaces.SynchronousExecutionMode

class LogNormal(vertices: Int, seed: Long = 0, sigma: Double = 1, mu: Double = 3) extends Traversable[(Int, Int)] {

  def foreach[U](f: ((Int, Int)) => U) = {
    val r = new Random(seed)
    var i = 0
    while (i < vertices) {
      val from = i
      val outDegree: Int = scala.math.exp(mu + sigma * (r.nextGaussian)).round.toInt //log-normal
      var j = 0
      while (j < outDegree) {
        val to = ((r.nextDouble * (vertices - 1))).round.toInt
        if (from != to) {
          f(from, to)
          j += 1
        }
      }
      i += 1
    }
  }

}

/** A simple benchmark to test how uch the generall performance of signal/collect has increased with each build. */
object Benchmark extends App {

  def buildPageRankGraph(cg: ComputeGraph, edgeTuples: Traversable[Tuple2[Int, Int]]): ComputeGraph = {
    edgeTuples foreach {
      case (sourceId, targetId) =>
        cg.add(new Page(sourceId, 0.85))
        cg.add(new Page(targetId, 0.85))
        cg.add(new Link(sourceId, targetId))
    }
    cg
  }

//  val et = new LogNormal(500 * 1000, 0, 1, 2.5)
  val et = new LogNormal(1000, 0, 1, 2.5)
  var evalGraph = buildPageRankGraph(DefaultBuilder.withNumberOfWorkers(24).build, et)
//  evalGraph.setSignalThreshold(0.001)
//  evalGraph.setCollectThreshold(0.0)
  var stats = evalGraph.execute
  var asynchronousComputationTime24Workers = stats.executionStatistics.computationTimeInMilliseconds
  val asynchronousPerformanceScore = 100.0 * 91424.0 / asynchronousComputationTime24Workers
  println("Asynchronous Performance Score:\t" + asynchronousPerformanceScore.toInt + "%")
  evalGraph.shutdown

//  evalGraph = buildPageRankGraph(DefaultBuilder.withNumberOfWorkers(24).build, et)
//  System.gc
////  evalGraph.setSignalThreshold(0.001)
////  evalGraph.setCollectThreshold(0.0)
//  stats = evalGraph.execute
//  val synchronousComputationTime24Workers = stats.executionStatistics.computationTimeInMilliseconds
//  val synchronousPerformanceScore = 100.0 * 91424.0 / synchronousComputationTime24Workers
//  println("Synchronous Performance Score:\t" + synchronousPerformanceScore.toInt + "%")
//  evalGraph.shutdown
//  
//  evalGraph = buildPageRankGraph(DefaultBuilder.withExecutionMode(SynchronousExecutionMode).withNumberOfWorkers(12).build, et)
//  System.gc
////  evalGraph.setSignalThreshold(0.001)
////  evalGraph.setCollectThreshold(0.0)
//  stats = evalGraph.execute
//  val asynchronousComputationTime12Workers = stats.executionStatistics.computationTimeInMilliseconds
//  evalGraph.shutdown
//  
//  val scalabilityScore = ((asynchronousComputationTime12Workers.toDouble / asynchronousComputationTime24Workers.toDouble) * 100.0) - 100.0
//  println("Asynchronous Scalability Score:\t" + scalabilityScore.toInt + "%")
//  evalGraph.shutdown

}