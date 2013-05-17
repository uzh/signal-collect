/*
 *  @author Daniel Strebel
 *
 *  Copyright 2012 University of Zurich
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
package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.configuration._

/**
 * Represents an entity in a Chinese Whispers clustering algorithm.
 * Each entity determines its new state i.e. the cluster it belongs to,
 * to be the most popular cluster in among its neighbors. Ties are broken randomly.
 * Initially each entity assumes it belongs to its own cluster and therefore uses
 * its own ID as cluster label.
 */
class CWVertex(id: Any, selfPreference: Double = 1.0) extends DataGraphVertex(id, id) {

  type Signal = (Any, Double)

  def collect = {
    //group most recent signals by clustering label
    val grouped = (((state, selfPreference)) :: signals.toList).groupBy(_._1)
    //sort the grouped list by the sum of all clustering label weights
    val sorted = grouped.toList sortBy { _._2.foldLeft(0.0)((sum, elem) => sum + elem._2) }
    //return the most popular label as new state
    sorted.last._1
  }
}

/**
 * Connects two entities in a Chinese Whispers algorithm and sets the signal to be
 * the source vertex's state plus together with the weight of the connection.
 */
class CWEdge(t: Any, weight: Double = 1.0) extends DefaultEdge(t) {
  type Source = CWVertex

  def signal = (source.state, weight)

}

/**
 * Exemplary algorithm of two fully connected clusters containing {0,1,2} and {8,9,10}
 * where nodes 2 and 8 are connected via a chain.
 */
object ChineseWhispersClustering extends App {

  val graph = GraphBuilder.build

  for (i <- 0 until 11) {
    graph.addVertex(new CWVertex(i))
  }

  graph.addVertex(new CWVertex(1))
  graph.addVertex(new CWVertex(2))
  graph.addVertex(new CWVertex(3))
  graph.addVertex(new CWVertex(4))
  graph.addVertex(new CWVertex(5))

  for (i <- 0 until 2) {
    graph.addEdge(i, new CWEdge(i + 1))
    graph.addEdge(i + 1, new CWEdge(i))
  }

  graph.addEdge(0, new CWEdge(2))
  graph.addEdge(2, new CWEdge(0))

  for (i <- 2 until 8) {
    graph.addEdge(i, new CWEdge(i + 1))
    graph.addEdge(i + 1, new CWEdge(i))
  }

  for (i <- 8 until 10) {
    graph.addEdge(i, new CWEdge(i + 1))
    graph.addEdge(i + 1, new CWEdge(i))
  }

  graph.addEdge(10, new CWEdge(8))
  graph.addEdge(8, new CWEdge(10))

  val stats = graph.execute
  graph.foreachVertex { v => println(v) }

  println(stats)

  graph.shutdown
}