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

import scala.Array.canBuildFrom
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.future
import scala.language.postfixOps
import scala.util.Random

import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.EdgeId
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.WorkerApi
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.interfaces.WorkerStatistics.apply
import com.signalcollect.interfaces.NodeStatistics

/**
 * Class that allows to interact with all the workers as if there were just one worker.
 */
class DefaultWorkerApi[Id, Signal](
  val workers: Array[WorkerApi[Id, Signal]],
  val mapper: VertexToWorkerMapper[Id])
    extends WorkerApi[Id, Signal] {

  protected val random = new Random

  override def toString = "DefaultWorkerApi"

  protected def futures[G](f: WorkerApi[Id, Signal] => G) = {
    workers map (worker => future { f(worker) })
  }

  protected def get[G](f: Future[G]): G = Await.result(f, timeout)

  protected val timeout = 2 hours

  def getIndividualWorkerStatistics: List[WorkerStatistics] = futures(_.getWorkerStatistics) map get toList

  override def getWorkerStatistics: WorkerStatistics = {
    getIndividualWorkerStatistics.fold(WorkerStatistics())(_ + _)
  }

  // TODO: Move to node.
  def getIndividualNodeStatistics: List[NodeStatistics] = futures(_.getNodeStatistics) map get toList

  override def getNodeStatistics: NodeStatistics = {
    getIndividualNodeStatistics.fold(NodeStatistics())(_ + _)
  }

  override def signalStep: Boolean = futures(_.signalStep) forall get

  override def collectStep: Boolean = futures(_.collectStep) forall get

  override def startComputation = futures(_.startComputation) foreach get

  override def pauseComputation = futures(_.pauseComputation) foreach get

  override def recalculateScores = futures(_.recalculateScores) foreach get

  override def recalculateScoresForVertexWithId(vertexId: Id) = {
    workers(mapper.getWorkerIdForVertexId(vertexId)).recalculateScoresForVertexWithId(vertexId)
  }

  override def forVertexWithId[VertexType <: Vertex[Id, _], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType = {
    workers(mapper.getWorkerIdForVertexId(vertexId)).forVertexWithId(vertexId, f)
  }

  override def foreachVertex(f: (Vertex[Id, _]) => Unit) = futures(_.foreachVertex(f)) foreach get

  override def foreachVertexWithGraphEditor(f: GraphEditor[Id, Signal] => Vertex[Id, _] => Unit) = futures(_.foreachVertexWithGraphEditor(f)) foreach get

  override def aggregateOnWorker[WorkerResult](aggregationOperation: ComplexAggregation[WorkerResult, _]): WorkerResult = {
    throw new UnsupportedOperationException("DefaultWorkerApi does not support this operation.")
  }

  override def aggregateAll[WorkerResult, EndResult](aggregationOperation: ComplexAggregation[WorkerResult, EndResult]): EndResult = {
    // TODO: Identify and fix bug that appears on large graphs with the TopK aggregator when using futures.
    //val aggregateArray = futures(_.aggregateOnWorker(aggregationOperation)) map get
    val workerAggregates = workers.par map (_.aggregateOnWorker(aggregationOperation))
    aggregationOperation.aggregationOnCoordinator(workerAggregates.toList)
  }

  override def setUndeliverableSignalHandler(h: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit) = {
    futures(_.setUndeliverableSignalHandler(h)) foreach get
  }

  override def setSignalThreshold(t: Double) = futures(_.setSignalThreshold(t)) foreach get

  override def setCollectThreshold(t: Double) = futures(_.setCollectThreshold(t)) foreach get

  override def reset = futures(_.reset) foreach get

  //----------------GraphEditor, BLOCKING variant-------------------------

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  override def addVertex(vertex: Vertex[Id, _]) {
    workers(mapper.getWorkerIdForVertexId(vertex.id)).addVertex(vertex)
  }

  /**
   *  Adds `edge` to the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  override def addEdge(sourceId: Id, edge: Edge[Id]) {
    workers(mapper.getWorkerIdForVertexId(sourceId)).addEdge(sourceId, edge)
  }

  /**
   *  Processes `signal` on the worker that has the vertex with
   *  `vertex.id==edgeId.targetId`.
   *  Blocks until the operation has completed.
   */
  override def processSignal(signal: Signal, targetId: Id, sourceId: Option[Id]) {
    workers(mapper.getWorkerIdForVertexId(targetId)).processSignal(signal, targetId, sourceId)
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  override def removeVertex(vertexId: Id) {
    workers(mapper.getWorkerIdForVertexId(vertexId)).removeVertex(vertexId)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  override def removeEdge(edgeId: EdgeId[Id]) {
    workers(mapper.getWorkerIdForVertexId(edgeId.sourceId)).removeEdge(edgeId)
  }

  /**
   * Runs a graph loading function on a worker
   */
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None) {
    workers(workerIdForHint(vertexIdHint)).modifyGraph(graphModification)
  }

  /**
   *  Loads a graph using the provided iterator of `graphModification` functions.
   *
   *  @note Does not block.
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def loadGraph(graphModifications: Iterator[GraphEditor[Id, Signal] => Unit], vertexIdHint: Option[Id]) {
    workers(workerIdForHint(vertexIdHint)).loadGraph(graphModifications)
  }

  def snapshot = futures(_.snapshot) foreach get
  def restore = futures(_.restore) foreach get
  def deleteSnapshot = futures(_.deleteSnapshot) foreach get

  protected def workerIdForHint(vertexIdHint: Option[Id]): Int = {
    if (vertexIdHint.isDefined) {
      mapper.getWorkerIdForVertexId(vertexIdHint.get)
    } else {
      random.nextInt(workers.length)
    }
  }

}
