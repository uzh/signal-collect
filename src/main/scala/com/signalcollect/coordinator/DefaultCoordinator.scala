/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

package com.signalcollect.coordinator

import java.lang.management.ManagementFactory
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.reflect.ClassTag
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.Heartbeat
import com.signalcollect.interfaces.Logger
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageRecipientRegistry
import com.signalcollect.interfaces.NodeStatus
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.SentMessagesStats
import com.signalcollect.interfaces.SentMessagesStats
import com.signalcollect.interfaces.WorkerStatus
import com.signalcollect.messaging.AkkaProxy
import com.sun.management.OperatingSystemMXBean
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout
import akka.actor.actorRef2Scala
import com.signalcollect.interfaces.ActorRestartLogging

// special command for coordinator
case class OnIdle(action: (DefaultCoordinator[_, _], ActorRef) => Unit)

// special reply from coordinator
case class IsIdle(b: Boolean)

/**
 * Incrementor function needs to be defined in its own class to prevent unnecessary
 * closure capture when serialized.
 */
case object IncrementorForCoordinator {
  def increment(messageBus: MessageBus[_, _]) = {
    messageBus.incrementMessagesSentToCoordinator
  }
}

class DefaultCoordinator[Id: ClassTag, Signal: ClassTag](
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory,
  loggerRef: ActorRef,
  heartbeatIntervalInMilliseconds: Long) extends Actor
  with MessageRecipientRegistry
  with Coordinator[Id, Signal]
  with ActorLogging
  with ActorRestartLogging {

  /**
   * Timeout for Akka actor idling
   */
  context.setReceiveTimeout(Duration.Undefined)

  val logger = AkkaProxy.newInstance[Logger](loggerRef)

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](
      numberOfWorkers,
      numberOfNodes,
      IncrementorForCoordinator.increment _)
  }

  val heartbeatInterval = heartbeatIntervalInMilliseconds * 1000000 // milliseconds to nanoseconds

  var lastHeartbeatTimestamp = 0l

  var allWorkersInitialized = false

  def shouldSendHeartbeat: Boolean = {
    allWorkersInitialized && messageBus.isInitialized && (System.nanoTime - lastHeartbeatTimestamp) > heartbeatInterval
  }

  var globalQueueSizeLimitPreviousHeartbeat = 0l
  var globalReceivedMessagesPreviousHeartbeat = 0l

  def logMessages {
    log.debug("Idle: " + workerStatus.filter(workerStatus => workerStatus != null && workerStatus.isIdle).size + "/" + numberOfWorkers)
    log.debug(s"Workers sent to    : ${messagesSentToWorkers.toList}")
    log.debug(s"Workers received by: ${messagesReceivedByWorkers.toList}")
    log.debug(s"Nodes sent to      : ${messagesSentToNodes.toList}")
    log.debug(s"Nodes received by  : ${messagesReceivedByNodes.toList}")
    log.debug(s"Coordinator sent to: ${messagesSentToCoordinator}")
    log.debug(s"Coord. received by : ${messagesReceivedByCoordinator}")
    log.debug(s"Total sent         : ${totalMessagesSent}")
    log.debug(s"Total received     : ${totalMessagesReceived}")
    log.debug(s"Global inbox size  : ${getGlobalInboxSize}")
    log.debug(workerApi.getWorkerStatistics.toString)
    //    println("Worker RPC ...")
    //    println(s"Number of vertices loaded total: ${workerApi.getWorkerStatistics.numberOfVertices}")
    //    println("Worker RPC ...")
    //    println(s"Number of vertices loaded per worker: ${workerApi.getIndividualWorkerStatistics map (_.numberOfVertices) mkString (", ")}")
    //    println("Worker RPC ...")
    //    val individualSystemMemFree = workerApi.getIndividualSystemInformation map (_.jmx_mem_free)
    //    println(s"Worker with least amount of free memory: ${((individualSystemMemFree min) / 100000000.0).round / 10.0}GB")
    //    println(s"Free memory per worker: ${individualSystemMemFree map { x => ((x / 100000000.0).round / 10.0) + "GB" } mkString(", ")}")
    //    verboseIsIdle
  }

  def sendHeartbeat {
    //    log.debug("Coordinator is sending a heartbeat.")
    logMessages
    val currentGlobalQueueSize = getGlobalInboxSize
    val deltaPreviousToCurrent = currentGlobalQueueSize - globalQueueSizeLimitPreviousHeartbeat
    // Linear interpolation to predict future queue size.
    val predictedGlobalQueueSize = currentGlobalQueueSize + deltaPreviousToCurrent
    val currentMessagesReceived = totalMessagesReceived
    val currentThroughput = currentMessagesReceived - globalReceivedMessagesPreviousHeartbeat
    val globalQueueSizeLimit = (((currentThroughput + numberOfWorkers) * 1.2) + globalQueueSizeLimitPreviousHeartbeat) / 2
    val maySignal = predictedGlobalQueueSize <= globalQueueSizeLimit
    lastHeartbeatTimestamp = System.nanoTime
    messageBus.sendToWorkers(Heartbeat(maySignal), false)
    messageBus.sendToNodes(Heartbeat(maySignal), false)
    //    debug(s"maySignal=$maySignal")
    globalReceivedMessagesPreviousHeartbeat = currentMessagesReceived
    globalQueueSizeLimitPreviousHeartbeat = currentGlobalQueueSize
  }

  protected var workerStatus: Array[WorkerStatus] = new Array[WorkerStatus](numberOfWorkers)
  protected var nodeStatus: Array[NodeStatus] = new Array[NodeStatus](numberOfNodes)

  var nodeStatusReceived = 0
  var workerStatusReceived = 0

  def receive = {
    case ws: WorkerStatus =>
      log.debug(s"Coordinator received a worker status from worker ${ws.workerId}, the workers idle status is now: ${ws.isIdle}")
      messageBus.getReceivedMessagesCounter.incrementAndGet
      workerStatusReceived += 1
      updateWorkerStatusMap(ws)
      if (isIdle) {
        onIdle
      }
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case ns: NodeStatus =>
      log.debug(s"Coordinator received a node status from node ${ns.nodeId}")
      messageBus.getReceivedMessagesCounter.incrementAndGet
      nodeStatusReceived += 1
      updateNodeStatusMap(ns)
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case ReceiveTimeout =>
      log.debug("Coordinator got a receive timeout.")
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case OnIdle(action) =>
      log.debug(s"Coordinator received an OnIdle request from $sender")
      context.setReceiveTimeout(heartbeatIntervalInMilliseconds.milliseconds)
      // Not counting these messages, because they only come from the local graph.
      onIdleList = (sender, action) :: onIdleList
      if (isIdle) {
        onIdle
      }
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case Request(command, reply, incrementor) =>
      log.debug(s"Coordinator received a request.")
      try {
        val result = command.asInstanceOf[Coordinator[Id, Signal] => Any](this)
        if (reply) {
          incrementor(messageBus)
          if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException
            sender ! None
          } else {
            sender ! result
          }
        }
      } catch {
        case t: Throwable =>
          log.error(t.toString)
          throw t
      }
  }

  def updateWorkerStatusMap(ws: WorkerStatus) {
    // Only update worker status if no status received so far or if the current status is newer.
    if (workerStatus(ws.workerId) == null || workerStatus(ws.workerId).messagesSent.sumRelevant < ws.messagesSent.sumRelevant) {
      workerStatus(ws.workerId) = ws
      if (!allWorkersInitialized) {
        allWorkersInitialized = workerStatus forall (_ != null)
      }
    }
  }

  def updateNodeStatusMap(ns: NodeStatus) {
    // Only update node status if no status received so far or if the current status is newer.
    if (nodeStatus(ns.nodeId) == null || nodeStatus(ns.nodeId).messagesSent.sumRelevant < ns.messagesSent.sumRelevant) {
      nodeStatus(ns.nodeId) = ns
    }
  }

  def onIdle {
    context.setReceiveTimeout(Duration.Undefined)
    for ((from, action) <- onIdleList) {
      action(this, from)
    }
    onIdleList = List[(ActorRef, (DefaultCoordinator[Id, Signal], ActorRef) => Unit)]()
  }

  var waitingStart = System.nanoTime

  var onIdleList = List[(ActorRef, (DefaultCoordinator[Id, Signal], ActorRef) => Unit)]()

  protected lazy val workerApi = messageBus.getWorkerApi
  def getWorkerApi = workerApi

  protected lazy val graphEditor = messageBus.getGraphEditor
  def getGraphEditor = graphEditor

  def getWorkerStatuses: Array[WorkerStatus] = workerStatus.clone

  val messagesSentToWorkers: Array[Long] = new Array(numberOfWorkers)
  val messagesSentToNodes: Array[Long] = new Array(numberOfNodes)
  var messagesSentToCoordinator: Long = 0

  def totalMessagesSent = messagesSentToWorkers.sum + messagesSentToNodes.sum + messagesSentToCoordinator

  val messagesReceivedByWorkers: Array[Long] = new Array(numberOfWorkers)
  val messagesReceivedByNodes: Array[Long] = new Array(numberOfNodes)
  var messagesReceivedByCoordinator: Long = 0

  def totalMessagesReceived = messagesReceivedByWorkers.sum + messagesReceivedByNodes.sum + messagesReceivedByCoordinator

  def resetMessagingStats {
    for (workerId <- 0 until numberOfWorkers) {
      messagesSentToWorkers(workerId) = 0
      messagesReceivedByWorkers(workerId) = 0
    }
    for (nodeId <- 0 until numberOfNodes) {
      messagesSentToNodes(nodeId) = 0
      messagesReceivedByNodes(nodeId) = 0
    }
    messagesSentToCoordinator = 0
    messagesReceivedByCoordinator = 0
  }

  def allSentMessagesReceived: Boolean = {
    computeMessagingStats
    for (workerId <- 0 until numberOfWorkers) {
      if (messagesSentToWorkers(workerId) != messagesReceivedByWorkers(workerId)) {
        return false
      }
    }
    for (nodeId <- 0 until numberOfNodes) {
      if (messagesSentToNodes(nodeId) != messagesReceivedByNodes(nodeId)) {
        return false
      }
    }
    if (messagesSentToCoordinator != messagesReceivedByCoordinator) {
      return false
    }
    true
  }

  def computeMessagingStats {
    resetMessagingStats
    for (workerId <- 0 until numberOfWorkers) {
      val status = workerStatus(workerId)
      if (status != null) {
        messagesReceivedByWorkers(workerId) = status.messagesReceived
        updateSentMessages(status.messagesSent)
      }
    }
    for (nodeId <- 0 until numberOfNodes) {
      val status = nodeStatus(nodeId)
      if (status != null) {
        messagesReceivedByNodes(nodeId) = status.messagesReceived
        updateSentMessages(status.messagesSent)
      }
    }
    messagesReceivedByCoordinator = messageBus.messagesReceived
    val coordinatorMessagesSent = messagesSentByCoordinator
    updateSentMessages(coordinatorMessagesSent)
  }

  def updateSentMessages(stats: SentMessagesStats) {
    for (recipientId <- 0 until numberOfWorkers) {
      messagesSentToWorkers(recipientId) += stats.workers(recipientId)
    }
    for (recipientId <- 0 until numberOfNodes) {
      messagesSentToNodes(recipientId) += stats.nodes(recipientId)
    }
    messagesSentToCoordinator += stats.coordinator
  }

  def messagesSentByCoordinator = SentMessagesStats(
    messageBus.messagesSentToWorkers,
    messageBus.messagesSentToNodes,
    messageBus.messagesSentToCoordinator,
    messageBus.messagesSentToOthers)

  def getGlobalInboxSize: Long = totalMessagesSent - totalMessagesReceived

  def verboseIsIdle: Boolean = {
    val statusReceivedFromAllWorkers = workerStatus.forall(workerStatus => workerStatus != null)
    if (!statusReceivedFromAllWorkers) {
      log.debug("Coordinator not idle because not all workers have sent a status.")
      return false
    }
    val allIdle = workerStatus.forall(workerStatus => workerStatus.isIdle)
    if (!allIdle) {
      log.debug("Coordinator not idle because not all workers are idle.")
      return false
    }
    val allSentReceived = allSentMessagesReceived
    if (!allSentReceived) {
      log.debug("Coordinator not idle because not all sent messages were received.")
      return false
    }
    return true
  }

  def isIdle = workerStatus.forall(workerStatus => workerStatus != null && workerStatus.isIdle) && allSentMessagesReceived

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }

  def getLogMessages = {
    logger.getLogMessages
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
