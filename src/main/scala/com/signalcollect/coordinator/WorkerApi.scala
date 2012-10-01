/*
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

package com.signalcollect.coordinator

import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import scala.collection.parallel.mutable.ParArray
import scala.collection.JavaConversions._
import com.signalcollect.Edge
import com.signalcollect.Vertex
import com.signalcollect.GraphEditor
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorSystem
import akka.actor.Props
import com.signalcollect.serialization.DefaultSerializer
import scala.util.Random
import akka.dispatch.Future

/**
 * Class that allows to interact with all the workers as if there were just one worker.
 */
class WorkerApi(val workers: Array[Worker], val mapper: VertexToWorkerMapper) {

  override def toString = "WorkerApi"

  protected lazy val parallelWorkers = workers.par

  def getIndividualWorkerStatistics: List[WorkerStatistics] = {
    parallelWorkers.map(_.getWorkerStatistics).toList
  }

  def getWorkerStatistics: WorkerStatistics = {
    parallelWorkers.map(_.getWorkerStatistics).fold(WorkerStatistics(null))(_ + _)
  }

  def signalStep = parallelWorkers foreach (_.signalStep)

  def collectStep: Boolean = parallelWorkers.map(_.collectStep).reduce(_ && _)

  def startComputation = parallelWorkers foreach (_.startAsynchronousComputation)

  def pauseComputation = parallelWorkers foreach (_.pauseAsynchronousComputation)

  def recalculateScores = parallelWorkers foreach (_.recalculateScores)

  def recalculateScoresForVertexWithId(vertexId: Any) = workers(mapper.getWorkerIdForVertexId(vertexId)).recalculateScoresForVertexWithId(vertexId)

  def shutdown = parallelWorkers foreach (_.shutdown)

  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): ResultType = {
    workers(mapper.getWorkerIdForVertexId(vertexId)).forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex[_, _]) => Unit) = parallelWorkers foreach (_.foreachVertex(f))

  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]) = {
    val aggregateArray: ParArray[ValueType] = parallelWorkers map (_.aggregate(aggregationOperation))
    aggregateArray.fold(aggregationOperation.neutralElement)(aggregationOperation.aggregate(_, _))
  }

  def setUndeliverableSignalHandler(h: (SignalMessage[_], GraphEditor) => Unit) = parallelWorkers foreach (_.setUndeliverableSignalHandler(h))

  def setSignalThreshold(t: Double) = parallelWorkers foreach (_.setSignalThreshold(t))

  def setCollectThreshold(t: Double) = parallelWorkers foreach (_.setCollectThreshold(t))

  //----------------GraphEditor, BLOCKING variant-------------------------

  /**
   *  Sends `signal` to the vertex with `vertex.id==edgeId.targetId`.
   *  Blocks until the operation has completed.
   */
  def sendSignal(signal: Any, edgeId: EdgeId) {
    workers(mapper.getWorkerIdForVertexId(edgeId.targetId)).processSignal(SignalMessage(signal, edgeId))
  }

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex[_, _]) {
    workers(mapper.getWorkerIdForVertexId(vertex.id)).addVertex(DefaultSerializer.write(vertex))
  }

  /**
   *  Adds `edge` to the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(sourceId: Any, edge: Edge[_]) {
    val serializedEdge = DefaultSerializer.write(edge)
    workers(mapper.getWorkerIdForVertexId(sourceId)).addEdge(sourceId, serializedEdge)
  }

  /**
   *  Adds edges to vertices that satisfy `sourceVertexPredicate`. The edges added are created by `edgeFactory`,
   *  which will receive the respective vertex as a parameter.
   */
  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_]) {
    workers map (_.addPatternEdge(sourceVertexPredicate, edgeFactory))
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Any) {
    workers(mapper.getWorkerIdForVertexId(vertexId)).removeVertex(vertexId)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId) {
    workers(mapper.getWorkerIdForVertexId(edgeId.sourceId)).removeEdge(edgeId)
  }

  /**
   *  Removes all vertices that satisfy the `shouldRemove` predicate from the graph.
   */
  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) {
    workers map (_.removeVertices(shouldRemove))
  }

  /**
   * Runs a graph loading function on a worker
   */
  def loadGraph(vertexIdHint: Option[Any], graphLoader: GraphEditor => Unit) {
    if (vertexIdHint.isDefined) {
      val workerId = vertexIdHint.get.hashCode % workers.length
      workers(workerId).loadGraph(graphLoader)
    } else {
      val rand = new Random
      val randomWorkerId = rand.nextInt(workers.length)
      workers(randomWorkerId).loadGraph(graphLoader)
    }
  }
}