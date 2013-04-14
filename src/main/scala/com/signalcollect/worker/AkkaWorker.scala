/*
 *  @author Philip Stutz
 *  @author Francisco de Freitas
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

package com.signalcollect.worker

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Queue
import java.lang.management.ManagementFactory
import scala.Array.canBuildFrom
import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.interfaces.BulkSignal
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.EdgeId
import com.signalcollect.interfaces.Heartbeat
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.SystemInformation
import com.signalcollect.interfaces.WorkerActor
import com.signalcollect.interfaces.WorkerApi
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.interfaces.WorkerStatus
import com.signalcollect.serialization.DefaultSerializer
import com.sun.management.OperatingSystemMXBean
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.ReceiveTimeout
import akka.dispatch.MessageQueue

class WorkerOperationCounters(
  var messagesReceived: Long = 0l,
  var collectOperationsExecuted: Long = 0l,
  var signalOperationsExecuted: Long = 0l,
  var verticesAdded: Long = 0l,
  var verticesRemoved: Long = 0l,
  var outgoingEdgesAdded: Long = 0l,
  var outgoingEdgesRemoved: Long = 0l,
  var signalSteps: Long = 0l,
  var collectSteps: Long = 0l,
  var receiveTimeoutMessagesReceived: Long = 0l,
  var heartbeatMessagesReceived: Long = 0l,
  var signalMessagesReceived: Long = 0l,
  var bulkSignalMessagesReceived: Long = 0l,
  var continueMessagesReceived: Long = 0l,
  var requestMessagesReceived: Long = 0l,
  var otherMessagesReceived: Long = 0) {
  // Resets operation counters but not messages received/sent counters.
  def resetOperationCounters {
    collectOperationsExecuted = 0l
    signalOperationsExecuted = 0l
    verticesAdded = 0l
    verticesRemoved = 0l
    outgoingEdgesAdded = 0l
    outgoingEdgesRemoved = 0l
    signalSteps = 0l
    collectSteps = 0l
  }
}

object ContinueSignaling

class AkkaWorker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val workerId: Int,
  val numberOfWorkers: Int,
  val messageBusFactory: MessageBusFactory,
  val storageFactory: StorageFactory,
  val heartbeatIntervalInMilliseconds: Long)
  extends WorkerActor[Id, Signal] with ActorLogging {

  override def toString = "Worker" + workerId

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](numberOfWorkers)
  }

  var maySignal = true

  var continueSignalingReceived = true
  var awaitingContinueSignaling = false

  /**
   * Timeout for Akka actor idling
   */
  context.setReceiveTimeout(10 milliseconds)

  def isInitialized = messageBus.isInitialized

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {

    case PoisonPill =>
      shutdown

    /**
     * ReceiveTimeout message only gets sent after Akka actor mailbox has been empty for "receiveTimeout" milliseconds
     */
    case ReceiveTimeout =>
      counters.receiveTimeoutMessagesReceived += 1
      if (isConverged || isPaused) { // if the actor has nothing to compute and the mailbox is empty, then it is idle
        setIdle(true)
      } else {
        handlePauseAndContinue
        performComputations
      }

    case Heartbeat(maySignal) =>
      counters.heartbeatMessagesReceived += 1
      sendStatusToCoordinator
      this.maySignal = maySignal
      if (isConverged || isPaused) { // TODO: refactor code if this works
        setIdle(true)
      } else {
        handlePauseAndContinue
        performComputations
      }

    case msg =>
      setIdle(false)
      sendStatusToCoordinator
      process(msg) // process the message
      handlePauseAndContinue
      performComputations
  }

  val messageQueue: Queue[_] = context.asInstanceOf[{ def mailbox: { def messageQueue: MessageQueue } }].mailbox.messageQueue.asInstanceOf[{ def queue: Queue[_] }].queue

  def performComputations {
    if (!isPaused) {
      scheduleOperations
    }
  }

  val batchProcessSize = 10000

  def scheduleOperations {
    if (messageQueue.isEmpty) {
      val collected = vertexStore.toCollect.process(
        vertex => {
          executeCollectOperationOfVertex(vertex, addToSignal = false)
          if (vertex.scoreSignal > signalThreshold) {
            executeSignalOperationOfVertex(vertex)
          }
        })
    }
    if (!vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty && messageQueue.isEmpty) {
      vertexStore.toSignal.process(executeSignalOperationOfVertex(_))
    }
    messageBus.flush
  }

  protected val counters = new WorkerOperationCounters()
  protected val graphEditor: GraphEditor[Id, Signal] = new WorkerGraphEditor(workerId, this, messageBus)
  protected val vertexGraphEditor = graphEditor.asInstanceOf[GraphEditor[Any, Any]] // Vertex graph edits are not typesafe.
  protected var undeliverableSignalHandler: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit = (s, tId, sId, ge) => {}

  protected def process(message: Any) {
    counters.messagesReceived += 1
    message match {
      case s: SignalMessage[Id, Signal] =>
        counters.signalMessagesReceived += 1
        processSignal(s.signal, s.targetId, s.sourceId)
      case bulkSignal: BulkSignal[Id, Signal] =>
        counters.bulkSignalMessagesReceived += 1
        val size = bulkSignal.signals.length
        var i = 0
        if (bulkSignal.sourceIds != null) {
          while (i < size) {
            val sourceId = bulkSignal.sourceIds(i)
            if (sourceId != null) {
              processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), Some(sourceId))
            } else {
              processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
            }
            i += 1
          }
        } else {
          while (i < size) {
            processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
            i += 1
          }
        }
      case ContinueSignaling =>
        continueSignalingReceived = true
        awaitingContinueSignaling = false
      case Request(command, reply) =>
        counters.requestMessagesReceived += 1
        try {
          val result = command.asInstanceOf[WorkerApi[Id, Signal] => Any](this)
          if (reply) {
            if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException 
              messageBus.sendToActor(sender, None)
            } else {
              messageBus.sendToActor(sender, result)
            }
          }
        } catch {
          case t: Throwable =>
            log.error(t.toString)
            throw t
        }
      case other =>
        counters.otherMessagesReceived += 1
        log.warning("Could not handle message " + message)
    }
  }

  override def addVertex(vertex: Vertex[Id, _]) {
    if (vertexStore.vertices.put(vertex)) {
      counters.verticesAdded += 1
      counters.outgoingEdgesAdded += vertex.edgeCount
      vertex.afterInitialization(vertexGraphEditor)
      if (vertex.scoreSignal > signalThreshold) {
        vertexStore.toSignal.put(vertex)
      }
    } else {
      val existing = vertexStore.vertices.get(vertex.id)
    }
  }

  override def addEdge(sourceId: Id, edge: Edge[Id]) {
    val vertex = vertexStore.vertices.get(sourceId)
    if (vertex != null) {
      if (vertex.addEdge(edge, vertexGraphEditor)) {
        counters.outgoingEdgesAdded += 1
        if (vertex.scoreSignal > signalThreshold) {
          vertexStore.toSignal.put(vertex)
        }
      }
    } else {
      log.warning("Did not find vertex with id " + sourceId + " when trying to add outgoing edge (" + sourceId + ", " + edge.targetId + ")")
    }
  }

  override def removeEdge(edgeId: EdgeId[Id]) {
    val vertex = vertexStore.vertices.get(edgeId.sourceId)
    if (vertex != null) {
      if (vertex.removeEdge(edgeId.targetId, vertexGraphEditor)) {
        counters.outgoingEdgesRemoved += 1
        if (vertex.scoreSignal > signalThreshold) {
          vertexStore.toSignal.put(vertex)
        }
      } else {
        log.warning("Outgoing edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      log.warning("Source vertex not found found when trying to remove outgoing edge with id " + edgeId)
    }
  }

  override def removeVertex(vertexId: Id) {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      processRemoveVertex(vertex)
    } else {
      log.warning("Should remove vertex with id " + vertexId + ": could not find this vertex.")
    }
  }

  protected def processRemoveVertex(vertex: Vertex[Id, _]) {
    val edgesRemoved = vertex.removeAllEdges(vertexGraphEditor)
    counters.outgoingEdgesRemoved += edgesRemoved
    counters.verticesRemoved += 1
    vertex.beforeRemoval(vertexGraphEditor)
    vertexStore.vertices.remove(vertex.id)
    vertexStore.toCollect.remove(vertex.id)
    vertexStore.toSignal.remove(vertex.id)
  }

  def modifyGraph(graphLoader: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id]) {
    graphLoader(graphEditor)
  }

  def setUndeliverableSignalHandler(h: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit) {
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

  def recalculateScoresForVertexWithId(vertexId: Id) {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      recalculateVertexScores(vertex)
    }
  }

  protected def recalculateVertexScores(vertex: Vertex[Id, _]) {
    if (vertex.scoreCollect > collectThreshold) {
      vertexStore.toCollect.put(vertex)
    }
    if (vertex.scoreSignal > signalThreshold) {
      vertexStore.toSignal.put(vertex)
    }
  }

  def forVertexWithId[VertexType <: Vertex[Id, _], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType = {
    val vertex = vertexStore.vertices.get(vertexId)
    if (vertex != null) {
      val result = f(vertex.asInstanceOf[VertexType])
      result
    } else {
      throw new Exception("Vertex with id " + vertexId + " not found.")
    }
  }

  def foreachVertex(f: Vertex[Id, _] => Unit) {
    vertexStore.vertices.foreach(f)
  }

  override def aggregateOnWorker[WorkerResult](aggregationOperation: ComplexAggregation[WorkerResult, _]): WorkerResult = {
    aggregationOperation.aggregationOnWorker(vertexStore.vertices.stream)
  }

  override def aggregateAll[WorkerResult, EndResult](aggregationOperation: ComplexAggregation[WorkerResult, EndResult]): EndResult = {
    throw new UnsupportedOperationException("AkkaWorker does not support this operation.")
  }

  def startComputation {
    shouldStart = true
  }

  def pauseComputation {
    shouldPause = true
  }

  def signalStep: Boolean = {
    counters.signalSteps += 1
    vertexStore.toSignal.process(executeSignalOperationOfVertex(_))
    messageBus.flush
    vertexStore.toCollect.isEmpty
  }

  def collectStep: Boolean = {
    counters.collectSteps += 1
    vertexStore.toCollect.process(executeCollectOperationOfVertex(_))
    vertexStore.toSignal.isEmpty
  }

  def getIndividualWorkerStatistics = List(getWorkerStatistics)

  def getWorkerStatistics: WorkerStatistics = {
    WorkerStatistics(
      workerId = workerId,
      messagesReceived = counters.messagesReceived,
      messagesSent = messageBus.messagesSent,
      toSignalSize = vertexStore.toSignal.size,
      toCollectSize = vertexStore.toCollect.size,
      collectOperationsExecuted = counters.collectOperationsExecuted,
      signalOperationsExecuted = counters.signalOperationsExecuted,
      numberOfVertices = vertexStore.vertices.size,
      verticesAdded = counters.verticesAdded,
      verticesRemoved = counters.verticesRemoved,
      numberOfOutgoingEdges = counters.outgoingEdgesAdded - counters.outgoingEdgesRemoved, //only valid if no edges are removed during execution
      outgoingEdgesAdded = counters.outgoingEdgesAdded,
      outgoingEdgesRemoved = counters.outgoingEdgesRemoved,
      receiveTimeoutMessagesReceived = counters.receiveTimeoutMessagesReceived,
      heartbeatMessagesReceived = counters.heartbeatMessagesReceived,
      signalMessagesReceived = counters.signalMessagesReceived,
      bulkSignalMessagesReceived = counters.bulkSignalMessagesReceived,
      continueMessagesReceived = counters.continueMessagesReceived,
      requestMessagesReceived = counters.requestMessagesReceived,
      otherMessagesReceived = counters.otherMessagesReceived)
  }

  def shutdown {}

  protected var shouldShutdown = false
  protected var isIdle = false
  protected var isPaused = true
  protected var shouldPause = false
  protected var shouldStart = false

  protected var signalThreshold = 0.001
  protected var collectThreshold = 0.0

  protected var lastStatusUpdate = System.currentTimeMillis

  protected var vertexStore = storageFactory.createInstance[Id]

  protected def isConverged =
    vertexStore.toCollect.isEmpty && vertexStore.toSignal.isEmpty

  protected def getWorkerStatus: WorkerStatus = {
    WorkerStatus(
      workerId = workerId,
      isIdle = isIdle,
      isPaused = isPaused,
      messagesSent = messageBus.messagesSent,
      messagesReceived = counters.messagesReceived,
      workerStatistics = getWorkerStatistics,
      systemInformation = getSystemInformation)
  }

  protected def sendStatusToCoordinator {
    val currentTime = System.currentTimeMillis
    if (isInitialized && currentTime - lastStatusUpdate > heartbeatIntervalInMilliseconds) {
      lastStatusUpdate = currentTime
      val status = getWorkerStatus
      messageBus.sendToCoordinator(status)
    }
  }

  protected def setIdle(newIdleState: Boolean) {
    if (isInitialized && isIdle != newIdleState) {
      isIdle = newIdleState
      sendStatusToCoordinator
    }
  }

  protected def executeCollectOperationOfVertex(vertex: Vertex[Id, _], addToSignal: Boolean = true) {
    counters.collectOperationsExecuted += 1
    vertex.executeCollectOperation(vertexGraphEditor)
    if (addToSignal && vertex.scoreSignal > signalThreshold) {
      vertexStore.toSignal.put(vertex)
    }
  }

  protected def executeSignalOperationOfVertex(vertex: Vertex[Id, _]) {
    counters.signalOperationsExecuted += 1
    vertex.executeSignalOperation(vertexGraphEditor)
  }

  def processSignal(signal: Signal, targetId: Id, sourceId: Option[Id]) {
    val vertex = vertexStore.vertices.get(targetId)
    if (vertex != null) {
      if (vertex.deliverSignal(signal, sourceId)) {
        counters.collectOperationsExecuted += 1
        if (vertex.scoreSignal > signalThreshold) {
          vertexStore.toSignal.put(vertex)
        }
      } else {
        if (vertex.scoreCollect > collectThreshold) {
          vertexStore.toCollect.put(vertex)
        }
      }
    } else {
      undeliverableSignalHandler(signal, targetId, sourceId, graphEditor)
    }
  }

  def registerWorker(workerId: Int, worker: ActorRef) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerCoordinator(coordinator: ActorRef) {
    messageBus.registerCoordinator(coordinator)
  }

  def registerLogger(logger: ActorRef) {
    messageBus.registerLogger(logger)
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

  def reset {
    maySignal = true
    continueSignalingReceived = true
    awaitingContinueSignaling = false
    shouldShutdown = false
    isIdle = false
    isPaused = true
    shouldPause = false
    shouldStart = false
    lastStatusUpdate = System.currentTimeMillis
    counters.resetOperationCounters
    vertexStore = storageFactory.createInstance[Id]
    messageBus.reset
  }

  def getIndividualSystemInformation = List(getSystemInformation)

  def getSystemInformation: SystemInformation = {
    val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean]);
    val runtime: Runtime = Runtime.getRuntime()
    SystemInformation(
      workerId = workerId,
      os = System.getProperty("os.name"),
      runtime_mem_total = runtime.totalMemory(),
      runtime_mem_max = runtime.maxMemory(),
      runtime_mem_free = runtime.freeMemory(),
      runtime_cores = runtime.availableProcessors(),
      jmx_committed_vms = osBean.getCommittedVirtualMemorySize(),
      jmx_mem_free = osBean.getFreePhysicalMemorySize(),
      jmx_mem_total = osBean.getTotalPhysicalMemorySize(),
      jmx_swap_free = osBean.getFreeSwapSpaceSize(),
      jmx_swap_total = osBean.getTotalSwapSpaceSize(),
      jmx_process_load = osBean.getProcessCpuLoad(),
      jmx_process_time = osBean.getProcessCpuTime(),
      jmx_system_load = osBean.getSystemCpuLoad()
    )
  }

  /**
   * Creates a snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   * Overwrites any previous snapshot that might exist.
   */
  def snapshot {
    // Overwrites previous file if it should exist.
    val snapshotFileOutput = new DataOutputStream(new FileOutputStream(s"$workerId.snapshot"))
    vertexStore.vertices.foreach { vertex =>
      val bytes = DefaultSerializer.write(vertex)
      snapshotFileOutput.writeInt(bytes.length)
      snapshotFileOutput.write(bytes)
    }
    snapshotFileOutput.close
  }

  /**
   * Restores the last snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   */
  def restore {
    reset
    val maxSerializedSize = 64768
    val snapshotFile = new File(s"$workerId.snapshot")
    val buffer = new Array[Byte](maxSerializedSize)
    if (snapshotFile.exists) {
      val snapshotFileInput = new DataInputStream(new FileInputStream(snapshotFile))
      val buffer = new Array[Byte](maxSerializedSize)
      while (snapshotFileInput.available > 0) {
        val serializedLength = snapshotFileInput.readInt
        assert(serializedLength <= maxSerializedSize)
        val bytesRead = snapshotFileInput.read(buffer, 0, serializedLength)
        assert(bytesRead == serializedLength)
        val vertex = DefaultSerializer.read[Vertex[Id, _]](buffer)
        addVertex(vertex)
      }
      snapshotFileInput.close
    }
  }

  /**
   * Deletes the worker snapshots if they exist.
   */
  def deleteSnapshot {
    val snapshotFile = new File(s"$workerId.snapshot")
    if (snapshotFile.exists) {
      snapshotFile.delete
    }
  }

}
