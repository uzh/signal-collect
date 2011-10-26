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
import com.signalcollect._
import com.signalcollect.implementations.serialization.DefaultSerializer
import com.signalcollect.implementations.graph.DefaultGraphEditor

class WorkerOperationCounters(
  var messagesReceived: Long = 0l,
  var collectOperationsExecuted: Long = 0l,
  var signalOperationsExecuted: Long = 0l,
  var verticesAdded: Long = 0l,
  var verticesRemoved: Long = 0l,
  var outgoingEdgesAdded: Long = 0l,
  var outgoingEdgesRemoved: Long = 0l,
  var incomingEdgesAdded: Long = 0l,
  var incomingEdgesRemoved: Long = 0l,
  var signalSteps: Long = 0l,
  var collectSteps: Long = 0l)

class LocalWorker(val workerId: Int,
  workerConfig: WorkerConfiguration,
  numberOfWorkers: Int,
  coordinator: Any,
  val loggingLevel: Int)
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
    workerConfig.messageBusFactory.createInstance(numberOfWorkers)
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
    try {
      while (!shouldShutdown) {

        // if necessary send status update to coordinator
        val currentTime = System.currentTimeMillis
        if (currentTime - lastStatusUpdate > updateInterval) {
          lastStatusUpdate = currentTime
          sendStatusToCoordinator
        }

        handleIdling
        // While the computation is in progress, alternately check the inbox and collect/signal
        if (!isPaused) {
          vertexStore.toSignal.foreach(executeSignalOperationOfVertex(_), true)
          vertexStore.toCollect.foreach((vertexId, uncollectedSignals) => {
            processInbox
            val collectExecuted = executeCollectOperationOfVertex(vertexId, uncollectedSignals, false)
            if (collectExecuted) {
              executeSignalOperationOfVertex(vertexId)
            }
          }, true)
        }
      }
    } catch {
      case t: Throwable => severe(t)
    }
  }

  protected val counters = new WorkerOperationCounters()
  protected val graphApi: GraphEditor = DefaultGraphEditor.createInstance(messageBus)
  protected var undeliverableSignalHandler: (SignalMessage[_, _, _], GraphEditor) => Unit = (s, g) => {}

  protected val updateInterval: Long = workerConfig.statusUpdateIntervalInMillis.getOrElse(Long.MaxValue)

  protected def process(message: Any) {
    counters.messagesReceived += 1
    message match {
      case s: SignalMessage[_, _, _] => {
        debug(s)
        processSignal(s)
      }
      case WorkerRequest(command) => {
        command(this)
      }
      case other => warning("Could not handle message " + message)
    }
  }

  def addVertex(vertex: Vertex) {
    if (vertexStore.vertices.put(vertex)) {
      counters.verticesAdded += 1
      counters.outgoingEdgesAdded += vertex.outgoingEdgeCount
      try {
        vertex.afterInitialization(messageBus)
      } catch {
        case t: Throwable => severe(t)
      }
      if (vertex.scoreSignal > signalThreshold) {
        vertexStore.toSignal.add(vertex.id)
      }
    }
  }

  def addOutgoingEdge(edge: Edge) {
    val key = edge.id.sourceId
    val vertex = vertexStore.vertices.get(key)
    if (vertex != null) {
      if (vertex.addOutgoingEdge(edge)) {
        counters.outgoingEdgesAdded += 1
        vertexStore.toCollect.addVertex(vertex.id)
        vertexStore.toSignal.add(vertex.id)
        vertexStore.vertices.updateStateOfVertex(vertex)
        val request = WorkerRequest((_.addIncomingEdge(edge)))
        messageBus.sendToWorkerForVertexId(request, edge.id.targetId)
      }
    } else {
      warning("Did not find vertex with id " + edge.id.sourceId + " when trying to add outgoing edge " + edge)
    }
  }

  def addIncomingEdge(serializedEdge: Array[Byte]) {
    val edge = DefaultSerializer.read[Edge](serializedEdge)
    addIncomingEdge(edge)
  }

  def addIncomingEdge(edge: Edge) {
    val key = edge.id.targetId
    val vertex = vertexStore.vertices.get(key)
    if (vertex != null) {
      if (vertex.addIncomingEdge(edge)) {
        counters.incomingEdgesAdded += 1
        vertexStore.vertices.updateStateOfVertex(vertex)
      }
    } else {
      warning("Did not find vertex with id " + edge.id.sourceId + " when trying to add incoming edge " + edge)
    }
  }

  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge) {
    vertexStore.vertices foreach { vertex =>
      if (sourceVertexPredicate(vertex)) {
        addOutgoingEdge(edgeFactory(vertex))
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

  def removeOutgoingEdge(edgeId: EdgeId[Any, Any]) {
    val vertex = vertexStore.vertices.get(edgeId.sourceId)
    if (vertex != null) {
      if (vertex.removeOutgoingEdge(edgeId)) {
        counters.outgoingEdgesRemoved += 1
        val request = WorkerRequest((_.removeIncomingEdge(edgeId)))
        messageBus.sendToWorkerForVertexId(request, edgeId.targetId)
        vertexStore.toCollect.addVertex(vertex.id)
        vertexStore.toSignal.add(vertex.id)
        vertexStore.vertices.updateStateOfVertex(vertex)
      } else {
        warning("Outgoing edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      warning("Source vertex not found found when trying to remove outgoing edge with id " + edgeId)
    }
  }

  def removeIncomingEdge(edgeId: EdgeId[Any, Any]) {
    val vertex = vertexStore.vertices.get(edgeId.targetId)
    if (vertex != null) {
      if (vertex.removeIncomingEdge(edgeId)) {
        counters.incomingEdgesRemoved += 1
        vertexStore.vertices.updateStateOfVertex(vertex)
      } else {
        warning("Incoming edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      warning("Source vertex not found found when trying to remove incoming edge with id " + edgeId)
    }
  }

  def removeVertices(shouldRemove: Vertex => Boolean) {
    vertexStore.vertices foreach { vertex =>
      if (shouldRemove(vertex)) {
        processRemoveVertex(vertex)
      }
    }
  }

  protected def processRemoveVertex(vertex: Vertex) {
    counters.outgoingEdgesRemoved += vertex.outgoingEdgeCount
    val edgesRemoved = vertex.removeAllOutgoingEdges
    counters.outgoingEdgesRemoved += edgesRemoved
    counters.verticesRemoved += 1
    vertex.beforeRemoval(messageBus)
    vertexStore.vertices.remove(vertex.id)
  }

  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphEditor) => Unit) {
    undeliverableSignalHandler = h
  }

  def setSignalThreshold(st: Double) {
    signalThreshold = st
  }

  def setCollectThreshold(ct: Double) {
    collectThreshold = ct
  }

  def recalculateScores {
    vertexStore.vertices.foreach(recalculateVertexScores(_))
  }

  def recalculateScoresForVertexWithId(vertexId: Any) {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      recalculateVertexScores(vertex)
    }
  }

  protected def recalculateVertexScores(vertex: Vertex) {
    vertexStore.toCollect.addVertex(vertex.id)
    vertexStore.toSignal.add(vertex.id)
  }

  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    var result: Option[ResultType] = None
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null && vertex.isInstanceOf[VertexType]) {
      result = Some(f(vertex.asInstanceOf[VertexType]))
      vertexStore.vertices.updateStateOfVertex(vertex)
    }
    result
  }

  def foreachVertex(f: Vertex => Unit) {
    vertexStore.vertices.foreach(f)
  }

  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]): ValueType = {
    var acc = aggregationOperation.neutralElement
    vertexStore.vertices.foreach { vertex => acc = aggregationOperation.aggregate(acc, aggregationOperation.extract(vertex)) }
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
    vertexStore.toSignal foreach (executeSignalOperationOfVertex(_), true)
  }

  def collectStep: Boolean = {
    counters.collectSteps += 1
    vertexStore.toCollect foreach ((vertexId, uncollectedSignalsList) => {
      executeCollectOperationOfVertex(vertexId, uncollectedSignalsList)
    }, true)
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
      numberOfOutgoingEdges = counters.outgoingEdgesAdded - counters.outgoingEdgesRemoved, //only valid if no edges are removed during execution
      outgoingEdgesAdded = counters.outgoingEdgesAdded,
      outgoingEdgesRemoved = counters.outgoingEdgesRemoved)
  }

  def shutdown {
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

  protected var lastStatusUpdate = System.currentTimeMillis

  protected lazy val vertexStore = workerConfig.storageFactory.createInstance

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

  override protected def processInbox {
    var message = messageInbox.poll(0, TimeUnit.NANOSECONDS)
    while (message != null) {
      process(message)
      message = messageInbox.poll(0, TimeUnit.NANOSECONDS)
    }
  }

  protected def executeCollectOperationOfVertex(vertexId: Any, uncollectedSignalsList: Iterable[SignalMessage[_, _, _]], addToSignal: Boolean = true): Boolean = {
    var hasCollected = false
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      if (vertex.scoreCollect(uncollectedSignalsList) > collectThreshold) {
        try {
          counters.collectOperationsExecuted += 1
          vertex.executeCollectOperation(uncollectedSignalsList, messageBus)
          vertexStore.vertices.updateStateOfVertex(vertex)
          hasCollected = true
          if (addToSignal && vertex.scoreSignal > signalThreshold) {
            vertexStore.toSignal.add(vertex.id)
          }
        } catch {
          case t: Throwable => severe(t)
        }
      }
    } else {
      uncollectedSignalsList.foreach(undeliverableSignalHandler(_, graphApi))
    }
    hasCollected
  }

  protected def executeSignalOperationOfVertex(vertexId: Any): Boolean = {
    var hasSignaled = false
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      if (vertex.scoreSignal > signalThreshold) {
        try {
          vertex.executeSignalOperation(messageBus)
          counters.signalOperationsExecuted += 1
          vertexStore.vertices.updateStateOfVertex(vertex)
          hasSignaled = true
        } catch {
          case t: Throwable => severe(t)
        }
      }
    }
    hasSignaled
  }

  protected def processSignal(signal: SignalMessage[_, _, _]) {
    vertexStore.toCollect.addSignal(signal)
  }

  def registerWorker(workerId: Int, worker: Any) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerCoordinator(coordinator: Any) {
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