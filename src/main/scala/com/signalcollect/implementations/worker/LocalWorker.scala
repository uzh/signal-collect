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

package com.signalcollect.implementations.worker

import com.signalcollect.configuration._
import com.signalcollect.implementations.messaging.AbstractMessageRecipient
import java.util.concurrent.TimeUnit
import com.signalcollect.implementations._
import com.signalcollect.interfaces._
import java.util.concurrent.BlockingQueue
import java.util.HashSet
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set
import com.signalcollect.implementations.graph.DefaultGraphApi
import com.signalcollect.implementations.serialization.DefaultSerializer
import com.signalcollect.implementations.coordinator.WorkerApi

class WorkerOperationCounters(
  var messagesReceived: Long = 0l,
  var collectOperationsExecuted: Long = 0l,
  var signalOperationsExecuted: Long = 0l,
  var verticesAdded: Long = 0l,
  var verticesRemoved: Long = 0l,
  var outgoingEdgesAdded: Long = 0l,
  var outgoingEdgesRemoved: Long = 0l,
  var signalSteps: Long = 0l,
  var collectSteps: Long = 0l)

class LocalWorker(
  val workerId: Int,
  config: Configuration,
  coordinator: WorkerApi,
  mapper: VertexToWorkerMapper)
  extends AbstractMessageRecipient[Any]
  with Worker
  with Logging
  with Runnable {

  override def toString = "Worker" + workerId

  /**
   * ******************
   * MESSAGE BUS INIT *
   * ******************
   */
  val messageBus: MessageBus[Any] = {
    config.workerConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
  }

  messageBus.registerCoordinator(coordinator)

  /********************/

  /**
   * Generalization of worker initialization
   */
  def initialize {
    new Thread(this, toString).start
  }

  def run {
    while (!shouldShutdown) {
      handleIdling
      // While the computation is in progress, alternately check the inbox and collect/signal
      if (!isPaused) {
        vertexStore.toSignal.foreach(signal(_))
        vertexStore.toCollect.foreach { (vertexId, uncollectedSignalsList) =>
          processInbox
          if (collect(vertexId, uncollectedSignalsList)) {
            signal(vertexId)
          }
        }
      }
    }
  }

  protected val counters = new WorkerOperationCounters()
  protected val graphApi = DefaultGraphApi.createInstance(messageBus)
  protected var undeliverableSignalHandler: (Signal[_, _, _], GraphApi) => Unit = (s, g) => {}

  protected def process(message: Any) {
    counters.messagesReceived += 1
    message match {
      case s: Signal[_, _, _] => processSignal(s)
      case WorkerRequest(command) => command(this)
      case other => warning("Could not handle message " + message)
    }
  }

  def addVertex(serializedVertex: Array[Byte]) {
    val vertex = DefaultSerializer.read[Vertex[_, _]](serializedVertex)
    addVertex(vertex)
  }

  protected def addVertex(vertex: Vertex[_, _]) {
    if (vertexStore.vertices.put(vertex)) {
      counters.verticesAdded += 1
      vertex.afterInitialization(messageBus)
    }
  }

  def addEdge(serializedEdge: Array[Byte]) {
    val edge = DefaultSerializer.read[Edge[_, _]](serializedEdge)
    addEdge(edge)
  }

  protected def addEdge(edge: Edge[_, _]) {
    val key = edge.id._1
    val vertex = vertexStore.vertices.get(key)
    if (vertex != null) {
      counters.outgoingEdgesAdded += 1
      vertex.addOutgoingEdge(edge)
      vertexStore.toCollect.addVertex(vertex.id)
      vertexStore.toSignal.add(vertex.id)
      vertexStore.vertices.updateStateOfVertex(vertex)
    } else {
      warning("Did not find vertex with id " + edge.id._1 + " when trying to add edge " + edge)
    }
  }

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_, _]) {
    vertexStore.vertices foreach { vertex =>
      if (sourceVertexPredicate(vertex)) {
        addEdge(edgeFactory(vertex))
      }
    }
  }

  def removeVertex(vertexId: Any) {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      processRemoveVertex(vertex)
    } else {
      warning("Should remove vertex with id " + vertexId + ": could not find this vertex.")
    }
  }

  def removeOutgoingEdge(edgeId: (Any, Any, String)) {
    val vertex = vertexStore.vertices.get(edgeId._1)
    if (vertex != null) {
      if (vertex.removeOutgoingEdge(edgeId)) {
        counters.outgoingEdgesRemoved += 1
        vertexStore.vertices.updateStateOfVertex(vertex)
      } else {
        warning("Outgoing edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      warning("Source vertex not found found when trying to remove edge with id " + edgeId)
    }
  }

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) {
    vertexStore.vertices foreach { vertex =>
      if (shouldRemove(vertex)) {
        processRemoveVertex(vertex)
      }
    }
  }

  protected def processRemoveVertex(vertex: Vertex[_, _]) {
    counters.outgoingEdgesRemoved += vertex.outgoingEdgeCount
    val edgesRemoved = vertex.removeAllOutgoingEdges
    counters.outgoingEdgesRemoved += edgesRemoved
    counters.verticesRemoved += 1
    vertexStore.vertices.remove(vertex.id)
  }

  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit) {
    undeliverableSignalHandler = h
  }

  def setSignalThreshold(st: Double) {
    signalThreshold = st
  }

  def setCollectThreshold(ct: Double) {
    collectThreshold = ct
  }

  def recalculateScores {
    vertexStore.toSignal.clear
    vertexStore.toCollect.clear
    vertexStore.vertices.foreach(recalculateVertexScores(_))
  }

  def recalculateScoresForVertexWithId(vertexId: Any) {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      recalculateVertexScores(vertex)
    }
  }

  protected def recalculateVertexScores(vertex: Vertex[_, _]) {
    if (vertex.scoreCollect > collectThreshold) {
      vertexStore.toCollect.addVertex(vertex.id)
    }
    if (vertex.scoreSignal > signalThreshold) {
      vertexStore.toSignal.add(vertex.id)
    }
  }

  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    var result: Option[ResultType] = None
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null && vertex.isInstanceOf[VertexType]) {
      result = Some(f(vertex.asInstanceOf[VertexType]))
      vertexStore.vertices.updateStateOfVertex(vertex)
    }
    result
  }

  def foreachVertex(f: (Vertex[_, _]) => Unit) {
    vertexStore.vertices.foreach(f)
  }

  def aggregate[ValueType](neutralElement: ValueType, aggregator: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType = {
    var acc = neutralElement
    vertexStore.vertices.foreach { vertex => acc = aggregator(acc, extractor(vertex)) }
    acc
  }

  def startComputation {
    shouldStart = true
  }

  def pauseComputation {
    shouldPause = true
  }

  def signalStep {
    counters.signalSteps += 1
    vertexStore.toSignal foreach (signal(_))
  }

  def collectStep: Boolean = {
    counters.collectSteps += 1
    vertexStore.toCollect foreach { (vertexId, uncollectedSignalsList) =>
      collect(vertexId, uncollectedSignalsList);
      vertexStore.toSignal.add(vertexId)
    }
    vertexStore.toSignal.isEmpty
  }

  def getWorkerStatistics: WorkerStatistics = {
    WorkerStatistics(
      messagesReceived = counters.messagesReceived,
      messagesSent = messageBus.messagesSent,
      collectOperationsExecuted = counters.collectOperationsExecuted,
      signalOperationsExecuted = counters.signalOperationsExecuted,
      numberOfVertices = vertexStore.vertices.size,
      verticesAdded = counters.verticesAdded,
      verticesRemoved = counters.verticesRemoved,
      numberOfOutgoingEdges = {
        var outgoingEdgeCount = 0l
        foreachVertex(outgoingEdgeCount += _.outgoingEdgeCount)
        outgoingEdgeCount
      },
      outgoingEdgesAdded = counters.outgoingEdgesAdded,
      outgoingEdgesRemoved = counters.outgoingEdgesRemoved)
  }

  def shutdown = {
    vertexStore.cleanUp
    shouldShutdown = true
  }

  protected var shouldShutdown = false
  protected var isIdle = false
  protected var isPaused = true
  protected var shouldPause = false
  protected var shouldStart = false

  protected var signalThreshold = 0.001
  protected var collectThreshold = 0.0

  protected val idleTimeoutNanoseconds: Long = 1000l * 1000l * 5l // 5ms timeout

  protected lazy val vertexStore = config.workerConfiguration.storageFactory.createInstance(messageBus)

  protected def isConverged = vertexStore.toCollect.isEmpty && vertexStore.toSignal.isEmpty

  protected def getWorkerStatus: WorkerStatus = {
    WorkerStatus(
      workerId = workerId,
      isIdle = isIdle,
      isPaused = isPaused,
      messagesSent = messageBus.messagesSent,
      messagesReceived = counters.messagesReceived)
  }

  protected def sendStatusToCoordinator {
    val status = getWorkerStatus
    messageBus.sendToCoordinator(status)
  }

  protected def setIdle(newIdleState: Boolean) {
    if (isIdle != newIdleState) {
      isIdle = newIdleState
      sendStatusToCoordinator
    }
  }

  protected def processInboxOrIdle(idleTimeoutNanoseconds: Long) {
    var message: Any = messageInbox.poll(idleTimeoutNanoseconds, TimeUnit.NANOSECONDS)
    if (message == null) {
      setIdle(true)
      handleMessage
      setIdle(false)
    } else {
      process(message)
      processInbox
    }
  }

  override protected def collect(vertexId: Any, uncollectedSignalsList: List[Signal[_, _, _]]): Boolean = {
    var hasCollected = false
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      counters.collectOperationsExecuted += 1
      vertex.executeCollectOperation(uncollectedSignalsList, messageBus)
      vertexStore.toCollect.remove(vertex.id)
      vertexStore.vertices.updateStateOfVertex(vertex)
      hasCollected = true
    } else {
      uncollectedSignalsList.foreach(undeliverableSignalHandler(_, graphApi))
    }
    hasCollected
  }

  protected def signal(vertexId: Any): Boolean = {
    var hasSignaled = false
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      if (vertex.scoreSignal > signalThreshold) {
        counters.signalOperationsExecuted += 1
        vertex.executeSignalOperation(messageBus)
        vertexStore.vertices.updateStateOfVertex(vertex)
        hasSignaled = true
      }
    }
    hasSignaled
  }

  override protected def processSignal(signal: Signal[_, _, _]) {
    vertexStore.toCollect.addSignal(signal)
  }

  def registerWorker(workerId: Int, worker: Any) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerCoordinator(coordinator: MessageRecipient[Any]) {
    messageBus.registerCoordinator(coordinator)
  }

  protected def handlePauseAndContinue {
    if (shouldStart) {
      shouldStart = false
      isPaused = false
      sendStatusToCoordinator
    } else if (shouldPause) {
      shouldPause = false
      isPaused = true
      sendStatusToCoordinator
    }
  }

  protected def handleIdling {
    handlePauseAndContinue
    if (isConverged || isPaused) {
      processInboxOrIdle(idleTimeoutNanoseconds)
    } else {
      processInbox
    }
  }

}