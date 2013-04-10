/*
 *  @author Philip Stutz
 *  
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
import com.signalcollect.interfaces.WorkerActor
import com.signalcollect.interfaces.WorkerApi
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.interfaces.WorkerStatus
import com.signalcollect.serialization.DefaultSerializer
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.ReceiveTimeout
import akka.dispatch.MessageQueue

object ContinueOperations

/**
 * Class that interfaces the worker implementation with Akka messaging.
 * Mainly responsible for translating received messages to function calls on a worker implementation.
 * Also responsible for messages that are sent directly via message bus (low-level messaging).
 */
class AkkaWorker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val workerId: Int,
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  val messageBusFactory: MessageBusFactory,
  val storageFactory: StorageFactory,
  val heartbeatIntervalInMilliseconds: Int,
  val loggingLevel: Int)
    extends WorkerActor[Id, Signal] with ActorLogging {

  override def toString = "Worker" + workerId

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](numberOfWorkers, numberOfNodes)
  }

  val worker = new WorkerImplementation(
    workerId = workerId,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    maySignal = true, // If the coordinator allows this worker to signal.
    operationsOnHold = false, // If operations such as a bulk graph modification or signaling was interrupted to be continued later.
    awaitingContinue = false, // If the worker is currently awaiting a continue message.
    flushedAfterUndeliverableSignalHandler = true,
    isIdle = false,
    isPaused = true,
    signalThreshold = 0.001,
    collectThreshold = 0.0,
    undeliverableSignalHandler = (s, tId, sId, ge) => {})

  /**
   * The worker sends itself a continue message.
   * This is used in order to keep the worker responsive while processing a lot of signaling/loading operations.
   */
  def continueLaterIfNecessary {
    if (worker.operationsOnHold && !worker.awaitingContinue && !(vertexStore.toSignal.isEmpty && pendingModifications.isEmpty)) {
      messageBus.sendToActor(self, ContinueOperations)
      worker.awaitingContinue = true
    }
  }
 
  /**
   * How many graph modifications this worker will execute in one batch.
   */
  val graphModificationBatchProcessingSize = 100

  /**
   * Timeout for Akka actor idling
   */
  context.setReceiveTimeout(10 milliseconds)

  def isInitialized = messageBus.isInitialized

  val messageQueue: Queue[_] = context.asInstanceOf[{ def mailbox: { def messageQueue: MessageQueue } }].mailbox.messageQueue.asInstanceOf[{ def queue: Queue[_] }].queue

  protected def handlePauseAndContinue {
    if (shouldStart && worker.pendingModifications.isEmpty) {
      shouldStart = false
      isPaused = false
      sendStatusToCoordinator
    } else if (shouldPause) {
      shouldPause = false
      isPaused = true
      sendStatusToCoordinator
    }
  }

  def performComputations {
    if (!isPaused) {
      scheduleOperations
    } else {
      applyPendingGraphModifications
    }
  }

  def applyPendingGraphModifications {
    if (worker.pendingModifications.hasNext) {
      for (modification <- worker.pendingModifications.take(graphModificationBatchProcessingSize)) {
        modification(worker.graphEditor)
      }
      continueLaterIfNecessary
    }
  }

  def scheduleOperations {
    if (messageQueue.isEmpty) {
      val collected = vertexStore.toCollect.process(
        vertex => {
          executeCollectOperationOfVertex(vertex, addToSignal = false)
          if (vertex.scoreSignal > signalThreshold) {
            executeSignalOperationOfVertex(vertex)
          }
        })
      if (!vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty && messageQueue.isEmpty) {
        vertexStore.toSignal.process(executeSignalOperationOfVertex(_))
      }
      messageBus.flush
      worker.flushedAfterUndeliverableSignalHandler = true
    }
  }

  protected var undeliverableSignalHandler: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit = (s, tId, sId, ge) => {}

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {
    case msg =>
      setIdle(false) // TODO: Move into code for each message
      sendStatusToCoordinator
      process(msg) // process the message
      if (!worker.isConverged) {

      } else {
        handlePauseAndContinue
        performComputations
      }
  }

  protected def process(message: Any) {
    worker.counters.messagesReceived += 1
    message match {
      case s: SignalMessage[Id, Signal] =>
        worker.counters.signalMessagesReceived += 1
        processSignal(s.signal, s.targetId, s.sourceId)

      case bulkSignal: BulkSignal[Id, Signal] =>
        worker.counters.bulkSignalMessagesReceived += 1
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

      case ContinueOperations =>
        worker.operationsOnHold = false
        worker.awaitingContinue = false

      case Request(command, reply) =>
        worker.counters.requestMessagesReceived += 1
        try {
          val result = command.asInstanceOf[WorkerApi[Id, Signal] => Any](worker)
          if (reply) {
            if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException 
              messageBus.sendToActor(sender, None)
            } else {
              messageBus.sendToActor(sender, result)
            }
          }
        } catch {
          case e: Exception =>
            severe(e)
            throw e
        }

      case PoisonPill =>
        shutdown

      case ReceiveTimeout =>
        worker.counters.receiveTimeoutMessagesReceived += 1
        if (worker.isConverged || (isPaused && worker.pendingModifications.isEmpty)) { // if the actor has nothing to compute and the mailbox is empty, then it is idle
          setIdle(true)
        } else {
          handlePauseAndContinue
          performComputations
        }

      case Heartbeat(maySignal) =>
        worker.counters.heartbeatMessagesReceived += 1
        sendStatusToCoordinator
        worker.maySignal = maySignal
        if (worker.isConverged || (isPaused && worker.pendingModifications.isEmpty)) { // TODO: refactor code if this works
          setIdle(true)
        } else {
          handlePauseAndContinue
          performComputations
        }

      case other =>
        worker.counters.otherMessagesReceived += 1
        warning("Could not handle message " + message)
    }
  }

  protected var shouldShutdown = false
  protected var isIdle = false
  protected var isPaused = true
  protected var shouldPause = false
  protected var shouldStart = false

  protected var signalThreshold = 0.001
  protected var collectThreshold = 0.0

  protected var lastStatusUpdate = System.currentTimeMillis

  protected var vertexStore = storageFactory.createInstance[Id]

  protected def sendStatusToCoordinator {
    val currentTime = System.currentTimeMillis
    if (isInitialized && currentTime - lastStatusUpdate > heartbeatIntervalInMilliseconds) {
      lastStatusUpdate = currentTime
      val status = worker.getWorkerStatus
      messageBus.sendToCoordinator(status)
    }
  }

  protected def setIdle(newIdleState: Boolean) {
    if (isInitialized && isIdle != newIdleState) {
      isIdle = newIdleState
      sendStatusToCoordinator
    }
  }

  def registerWorker(workerId: Int, worker: ActorRef) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerNode(nodeId: Int, node: ActorRef) {
    messageBus.registerNode(nodeId, node)
  }

  def registerCoordinator(coordinator: ActorRef) {
    messageBus.registerCoordinator(coordinator)
  }

  def registerLogger(logger: ActorRef) {
    messageBus.registerLogger(logger)
  }

}