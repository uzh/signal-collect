/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
 *  @author Tobias Bachmann
 *
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.deployment

import java.io._
import scala.collection.mutable.ArrayBuffer
import com.signalcollect._
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import com.signalcollect.configuration.ExecutionMode._
import com.signalcollect.util.Ints
import com.signalcollect.util.IntSet
import akka.actor.ActorSystem
import akka.actor.ActorRef

/**
 * Use GraphSplitter to download the graph and generate the splits.
 *
 * Run with JVM parameters:
 * -Xmx2000m -Xms2000m
 *
 * Computation ran in as little as 2177 milliseconds (best run) on a notebook
 * with a 2.3GHz Core i7 (1 processor, 4 cores, 8 splits for 8 hyper-threads).
 */
class DeployableEfficientPageRankLoader extends DeployableAlgorithm {

  override def execute(parameters: Map[String, String], nodeActors: Option[Array[ActorRef]], actorSystem: Option[ActorSystem] = None) {
    val graphBuilder = if(nodeActors.isDefined)
      new GraphBuilder[Int, Double].withPreallocatedNodes(nodeActors.get)
      else new GraphBuilder[Int, Double]
    val graphBuilder2 = if(actorSystem.isDefined)
      graphBuilder.withActorSystem(actorSystem.get)
      else 
      graphBuilder
    val g = graphBuilder2.
      withMessageBusFactory(new BulkAkkaMessageBusFactory(1024, false)).
      //withMessageSerialization(true).
      build
    val numberOfSplits = Runtime.getRuntime.availableProcessors
    for (i <- 0 until numberOfSplits) {
      g.loadGraph(SplitLoader(s"web-split-$i"), Some(i))
    }
    print("Loading graph ...")
    g.awaitIdle
    println("done.")
    print("Running computation ...")
    val stats = g.execute(ExecutionConfiguration.withExecutionMode(PureAsynchronous).withSignalThreshold(0.01))
    println("done.")
    println(stats)
    val top100 = g.aggregate(new TopKFinder[Double](100))
    top100 foreach (println(_))
    g.shutdown

    //  def loadSplit(splitIndex: Int)(ge: GraphEditor[Int, Double]) {
    //    val in = splits(splitIndex)
    //    var vertexId = CompactIntSet.readUnsignedVarInt(in)
    //    while (vertexId >= 0) {
    //      val numberOfEdges = CompactIntSet.readUnsignedVarInt(in)
    //      var edges = new ArrayBuffer[Int]
    //      while (edges.length < numberOfEdges) {
    //        val nextEdge = CompactIntSet.readUnsignedVarInt(in)
    //        edges += nextEdge
    //      }
    //      val vertex = new EfficientPageRankVertex(vertexId)
    //      vertex.setTargetIds(edges.length, CompactIntSet.create(edges.toArray))
    //      ge.addVertex(vertex)
    //      vertexId = CompactIntSet.readUnsignedVarInt(in)
    //    }
    //  }
  }

}

case class SplitLoader(splitFileName: String) extends Iterator[GraphEditor[Int, Double] => Unit] {

  lazy val in = new DataInputStream(new FileInputStream(splitFileName))

  var loaded = 0

  def readNext: Int = Ints.readUnsignedVarInt(in)

  def nextEdges(length: Int): ArrayBuffer[Int] = {
    val edges = new ArrayBuffer[Int]
    while (edges.length < length) {
      val nextEdge = readNext
      edges += nextEdge
    }
    edges
  }

  def addVertex(vertex: Vertex[Int, Double])(graphEditor: GraphEditor[Int, Double]) {
    graphEditor.addVertex(vertex)
  }

  var vertexId = Int.MinValue

  def hasNext = {
    if (vertexId == Int.MinValue) {
      vertexId = readNext
    }
    vertexId >= 0
  }

  def next: GraphEditor[Int, Double] => Unit = {
    loaded += 1
    if (loaded % 10000 == 0) {
      println(loaded)
    }
    val numberOfEdges = readNext
    val edges = nextEdges(numberOfEdges)
    val vertex = new EfficientPageRankVertex(vertexId)
    vertex.setTargetIds(edges.length, Ints.createCompactSet(edges.toArray))
    vertexId = Int.MinValue
    addVertex(vertex) _
  }
}

/**
 * A version of PageRank that performs faster and uses less memory than the standard version.
 * This version signals only the deltas and collects upon signal delivery.
 */
class EfficientPageRankVertex(val id: Int) extends Vertex[Int, Double] {
  var state = 0.15
  var lastSignalState = 0.0
  var outEdges = 0.0
  def setState(s: Double) {
    state = s
  }
  protected var targetIdArray: Array[Byte] = null
  def setTargetIds(numberOfEdges: Int, compactIntSet: Array[Byte]) = {
    outEdges = numberOfEdges
    targetIdArray = compactIntSet
  }
  def deliverSignal(signal: Any, sourceId: Option[Any], graphEditor: GraphEditor[Any, Any]): Boolean = {
    val s = signal.asInstanceOf[Double]
    state = state + 0.85 * s
    true
  }
  override def executeSignalOperation(graphEditor: GraphEditor[Any, Any]) {
    if (outEdges != 0) {
      val signal = (state - lastSignalState) / outEdges
      new IntSet(targetIdArray).foreach((targetId: Int) =>
        graphEditor.sendSignal(signal, targetId, None))
    }
    lastSignalState = state
  }
  override def scoreSignal: Double = state - lastSignalState
  def scoreCollect = 0 // Because signals are collected upon delivery.
  def edgeCount = outEdges.toInt
  override def toString = s"${this.getClass.getName}(state=$state)"
  def executeCollectOperation(graphEditor: GraphEditor[Any, Any]) {}
  def afterInitialization(graphEditor: GraphEditor[Any, Any]) = {}
  def beforeRemoval(graphEditor: GraphEditor[Any, Any]) = {}
  override def addEdge(e: Edge[_], graphEditor: GraphEditor[Any, Any]): Boolean = throw new UnsupportedOperationException("Use setTargetIds(...)")
  override def removeEdge(targetId: Any, graphEditor: GraphEditor[Any, Any]): Boolean = throw new UnsupportedOperationException
  override def removeAllEdges(graphEditor: GraphEditor[Any, Any]): Int = throw new UnsupportedOperationException
}
