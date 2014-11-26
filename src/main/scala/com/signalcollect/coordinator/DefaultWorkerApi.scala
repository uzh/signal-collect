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

import java.util.concurrent.atomic.AtomicInteger

import scala.Array.canBuildFrom
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.EdgeId
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.NodeStatistics
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.WorkerApi
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.messaging.Command

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask

/**
 * Class that allows to interact with all the workers as if there were just one worker.
 */
class DefaultWorkerApi[Id, Signal](
  val incrementor: MessageBus[_, _] => Unit,
  val sentWorkerMessageCounters: Array[AtomicInteger],
  val receivedMessagesCounter: AtomicInteger,
  val workers: Array[ActorRef],
  val mapper: VertexToWorkerMapper[Id])
  extends WorkerApi[Id, Signal] {

  protected val random = new Random

  protected val numberOfWorkers = workers.length

  override def toString = "DefaultWorkerApi"

  protected def get[G](f: Future[G]): G = {
    Await.result(f, 2 hours)
  }

  protected def get[G](fs: Traversable[Future[G]]): List[G] = {
    val futureOfCollection = Future.sequence(fs.toList)
    Await.result(futureOfCollection, 2 hours)
  }

  protected implicit val timeout = new akka.util.Timeout(2 hours)

  protected def executeOnWorkerWithResult[Result](workerId: Int, command: Command[WorkerApi[Id, Signal]]): Result = {
    val request = Request(command, returnResult = true, incrementor)
    sentWorkerMessageCounters(workerId).incrementAndGet
    val resultFuture = (workers(workerId) ? request).asInstanceOf[Future[Result]]
    val result = get(resultFuture)
    receivedMessagesCounter.incrementAndGet
    result
  }

  protected def executeWithResult[Result](command: Command[WorkerApi[Id, Signal]]): List[Result] = {
    val request = Request(command, returnResult = true, incrementor)
    sentWorkerMessageCounters.foreach(_.incrementAndGet)
    val resultFutures = workers.map { w =>
      (w ? request).asInstanceOf[Future[Result]]
    }
    val result = get(resultFutures)
    receivedMessagesCounter.addAndGet(numberOfWorkers)
    result
  }

  protected def executeWithoutResult(command: Command[WorkerApi[Id, Signal]]) = {
    val request = Request(command, returnResult = false, incrementor)
    sentWorkerMessageCounters.foreach(_.incrementAndGet)
    val resultFutures = workers.map { w =>
      w ! request
    }
  }

  protected val workerApiInterface = "com.signalcollect.interfaces.WorkerApi"

  protected val getIndividualWorkerStatisticsCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract com.signalcollect.interfaces.WorkerStatistics com.signalcollect.interfaces.WorkerApi.getWorkerStatistics()", Array())
  }

  def getIndividualWorkerStatistics: List[WorkerStatistics] = {
    executeWithResult(getIndividualWorkerStatisticsCommand)
  }

  override def getWorkerStatistics: WorkerStatistics = {
    getIndividualWorkerStatistics.fold(WorkerStatistics())(_ + _)
  }

  protected val getIndividualNodeStatisticsCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract com.signalcollect.interfaces.NodeStatistics com.signalcollect.interfaces.WorkerApi.getNodeStatistics()", Array())
  }

  def getIndividualNodeStatistics: List[NodeStatistics] = {
    executeWithResult(getIndividualNodeStatisticsCommand)
  }

  override def getNodeStatistics: NodeStatistics = {
    getIndividualNodeStatistics.fold(NodeStatistics())(_ + _)
  }

  protected val signalStepCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract boolean com.signalcollect.interfaces.WorkerApi.signalStep()", Array())
  }

  override def signalStep = {
    val results = executeWithResult[Boolean](signalStepCommand)
    results.forall(_ == true)
  }

  protected val collectStepCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract boolean com.signalcollect.interfaces.WorkerApi.collectStep()", Array())
  }

  override def collectStep: Boolean = {
    val results = executeWithResult[Boolean](collectStepCommand)
    results.forall(_ == true)
  }

  protected val startComputationCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.startComputation()", Array())
  }

  override def startComputation {
    executeWithoutResult(startComputationCommand)
  }

  protected val pauseComputationCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.pauseComputation()", Array())
  }

  override def pauseComputation = {
    executeWithoutResult(pauseComputationCommand)
  }

  protected val recalculateScoresCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.recalculateScores()", Array())
  }

  override def recalculateScores = {
    executeWithoutResult(recalculateScoresCommand)
  }

  protected def recalculateScoresForVertexCommand(vertexId: Id) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.recalculateScoresForVertexWithId(java.lang.Object)", Array(vertexId.asInstanceOf[Object]))
  }

  override def recalculateScoresForVertexWithId(vertexId: Id) = {
    executeWithoutResult(recalculateScoresForVertexCommand(vertexId))
  }

  protected def forVertexWithIdCommand[VertexType <: Vertex[Id, _, Id, Signal], ResultType](vertexId: Id, f: VertexType => ResultType) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract java.lang.Object com.signalcollect.interfaces.WorkerApi.forVertexWithId(java.lang.Object,scala.Function1)", Array(vertexId.asInstanceOf[Object], f))
  }

  override def forVertexWithId[VertexType <: Vertex[Id, _, Id, Signal], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType = {
    val workerId = mapper.getWorkerIdForVertexId(vertexId)
    executeOnWorkerWithResult(workerId, forVertexWithIdCommand(vertexId, f))
  }

  protected def foreachVertexCommand(f: (Vertex[Id, _, Id, Signal]) => Unit) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.foreachVertex(scala.Function1)", Array(f))
  }

  override def foreachVertex(f: (Vertex[Id, _, Id, Signal]) => Unit) {
    executeWithoutResult(foreachVertexCommand(f))
  }

  protected def foreachVertexWithGraphEditorCommand(f: GraphEditor[Id, Signal] => Vertex[Id, _, Id, Signal] => Unit) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.foreachVertexWithGraphEditor(scala.Function1)", Array(f))
  }

  override def foreachVertexWithGraphEditor(f: GraphEditor[Id, Signal] => Vertex[Id, _, Id, Signal] => Unit) {
    executeWithoutResult(foreachVertexWithGraphEditorCommand(f))
  }

  override def aggregateOnWorker[WorkerResult](aggregationOperation: ComplexAggregation[WorkerResult, _]): WorkerResult = {
    throw new UnsupportedOperationException("DefaultWorkerApi does not support this operation.")
  }

  protected def aggregateOnWorkerCommand[WorkerResult, EndResult](aggregationOperation: ComplexAggregation[WorkerResult, EndResult]) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract java.lang.Object com.signalcollect.interfaces.WorkerApi.aggregateOnWorker(com.signalcollect.interfaces.ComplexAggregation)", Array(aggregationOperation))
  }

  override def aggregateAll[WorkerResult, EndResult](aggregationOperation: ComplexAggregation[WorkerResult, EndResult]): EndResult = {
    val workerAggregates = executeWithResult[WorkerResult](aggregateOnWorkerCommand(aggregationOperation))
    aggregationOperation.aggregationOnCoordinator(workerAggregates)
  }

  protected def setSignalThresholdCommand(t: Double) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.setSignalThreshold(double)", Array(t.asInstanceOf[Object]))
  }

  override def setSignalThreshold(t: Double) {
    executeWithoutResult(setSignalThresholdCommand(t))
  }

  protected def setCollectThresholdCommand(t: Double) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.setCollectThreshold(double)", Array(t.asInstanceOf[Object]))
  }

  override def setCollectThreshold(t: Double) = {
    executeWithoutResult(setCollectThresholdCommand(t))
  }

  protected val resetCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.reset()", Array())
  }

  override def reset {
    executeWithoutResult(resetCommand)
  }

  protected val initializeIdleDetectionCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.initializeIdleDetection()", Array())
  }

  override def initializeIdleDetection {
    executeWithoutResult(initializeIdleDetectionCommand)
  }

  //----------------GraphEditor, BLOCKING variant-------------------------

  protected def addVertexCommand(vertex: Vertex[Id, _, Id, Signal]) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.addVertex(com.signalcollect.Vertex)", Array(vertex))
  }

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  override def addVertex(vertex: Vertex[Id, _, Id, Signal]) {
    val workerId = mapper.getWorkerIdForVertexId(vertex.id)
    executeOnWorkerWithResult[Option[Unit]](workerId, addVertexCommand(vertex))
  }

  protected def addEdgeCommand(sourceId: Id, edge: Edge[Id]) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.addEdge(java.lang.Object,com.signalcollect.Edge)", Array(sourceId.asInstanceOf[Object], edge))
  }

  /**
   *  Adds `edge` to the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  override def addEdge(sourceId: Id, edge: Edge[Id]) {
    val workerId = mapper.getWorkerIdForVertexId(sourceId)
    executeOnWorkerWithResult[Option[Unit]](workerId, addEdgeCommand(sourceId, edge))
  }

  protected def processSignalWithSourceIdCommand(signal: Signal, targetId: Id, sourceId: Id) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.processSignalWithSourceId(java.lang.Object,java.lang.Object,java.lang.Object)",
      Array(signal.asInstanceOf[Object], targetId.asInstanceOf[Object], sourceId.asInstanceOf[Object]))
  }

  /**
   *  Processes `signal` on the worker that has the vertex with
   *  `vertex.id==edgeId.targetId`.
   *  Blocks until the operation has completed.
   */
  override def processSignalWithSourceId(signal: Signal, targetId: Id, sourceId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(targetId)
    executeOnWorkerWithResult[Option[Unit]](workerId, processSignalWithSourceIdCommand(signal, targetId, sourceId))
  }

  protected def processSignalWithoutSourceIdCommand(signal: Signal, targetId: Id) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.processSignalWithoutSourceId(java.lang.Object,java.lang.Object)",
      Array(signal.asInstanceOf[Object], targetId.asInstanceOf[Object]))
  }

  /**
   *  Processes `signal` on the worker that has the vertex with
   *  `vertex.id==edgeId.targetId`.
   *  Blocks until the operation has completed.
   */
  override def processSignalWithoutSourceId(signal: Signal, targetId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(targetId)
    executeOnWorkerWithResult[Option[Unit]](workerId, processSignalWithoutSourceIdCommand(signal, targetId))
  }

  protected def removeVertexCommand(vertexId: Id) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.removeVertex(java.lang.Object)",
      Array(vertexId.asInstanceOf[Object]))
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  override def removeVertex(vertexId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(vertexId)
    executeOnWorkerWithResult[Option[Unit]](workerId, removeVertexCommand(vertexId))
  }

  protected def removeEdgeCommand(edgeId: EdgeId[Id]) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.removeEdge(com.signalcollect.interfaces.EdgeId)", Array(edgeId))
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  override def removeEdge(edgeId: EdgeId[Id]) {
    val workerId = mapper.getWorkerIdForVertexId(edgeId.sourceId)
    executeOnWorkerWithResult[Option[Unit]](workerId, removeEdgeCommand(edgeId))
  }

  protected def modifyGraphCommand(graphModification: GraphEditor[Id, Signal] => Unit) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.modifyGraph(scala.Function1,scala.Option)", Array(graphModification))
  }

  /**
   * Runs a graph loading function on a worker
   */
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None) {
    val workerId = workerIdForHint(vertexIdHint)
    executeOnWorkerWithResult[Option[Unit]](workerId, modifyGraphCommand(graphModification))
  }

  protected def loadGraphCommand(graphModifications: Iterator[GraphEditor[Id, Signal] => Unit]) = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.loadGraph(scala.collection.Iterator,scala.Option)", Array(graphModifications, None))
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
    val workerId = workerIdForHint(vertexIdHint)
    executeOnWorkerWithResult[Option[Unit]](workerId, loadGraphCommand(graphModifications))
  }

  protected val snapshotCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.snapshot()", Array())
  }

  def snapshot {
    executeWithResult[Option[Unit]](snapshotCommand)
  }

  protected val restoreCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.restore()", Array())
  }

  def restore {
    executeWithResult[Option[Unit]](restoreCommand)
  }

  protected val deleteSnapshotCommand = {
    new Command[WorkerApi[Id, Signal]](workerApiInterface, "public abstract void com.signalcollect.interfaces.WorkerApi.deleteSnapshot()", Array())
  }

  def deleteSnapshot = {
    executeWithResult[Option[Unit]](deleteSnapshotCommand)
  }

  protected def workerIdForHint(vertexIdHint: Option[Id]): Int = {
    if (vertexIdHint.isDefined) {
      mapper.getWorkerIdForVertexId(vertexIdHint.get)
    } else {
      random.nextInt(workers.length)
    }
  }

}
