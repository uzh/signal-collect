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
import scala.reflect.ClassTag
import com.signalcollect.interfaces.ActorRestartLogging
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageRecipientRegistry
import com.signalcollect.interfaces.NodeStatus
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.SentMessagesStats
import com.signalcollect.interfaces.WorkerStatus
import com.sun.management.OperatingSystemMXBean
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import com.signalcollect.interfaces.BulkStatus
import com.signalcollect.worker.StatsDue

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
  throttlingEnabled: Boolean,
  messageBusFactory: MessageBusFactory[Id, Signal],
  mapperFactory: MapperFactory[Id]) extends Coordinator[Id, Signal]
  with Actor
  with ActorLogging
  with ActorRestartLogging
  with MessageRecipientRegistry {

  override def postStop {
    log.debug(s"Coordinator has stopped.")
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    val msg = s"Coordinator crashed with ${reason.toString} because of ${reason.getCause} or reason ${reason.getMessage} at position ${reason.getStackTrace.mkString("\n")}, not recoverable."
    println(msg)
    log.error(msg)
    context.stop(self)
  }

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance(
      context.system,
      numberOfWorkers,
      numberOfNodes,
      mapperFactory.createInstance(numberOfNodes, numberOfWorkers / numberOfNodes),
      IncrementorForCoordinator.increment _)
  }

  var allWorkersInitialized = false

  //  def logMessages {
  //    log.debug("Idle: " + workerStatus.filter(workerStatus => workerStatus != null && workerStatus.isIdle).size + "/" + numberOfWorkers)
  //    log.debug(s"Workers sent to    : ${messagesSentToWorkers.toList}")
  //    log.debug(s"Workers received by: ${messagesReceivedByWorkers.toList}")
  //    log.debug(s"Nodes sent to      : ${messagesSentToNodes.toList}")
  //    log.debug(s"Nodes received by  : ${messagesReceivedByNodes.toList}")
  //    log.debug(s"Coordinator sent to: ${messagesSentToCoordinator}")
  //    log.debug(s"Coord. received by : ${messagesReceivedByCoordinator}")
  //    log.debug(s"Total sent         : ${totalMessagesSent}")
  //    log.debug(s"Total received     : ${totalMessagesReceived}")
  //    log.debug(s"Global inbox size  : ${getGlobalInboxSize}")
  //    log.debug(workerApi.getWorkerStatistics.toString)
  //    //    println("Worker RPC ...")
  //    //    println(s"Number of vertices loaded total: ${workerApi.getWorkerStatistics.numberOfVertices}")
  //    //    println("Worker RPC ...")
  //    //    println(s"Number of vertices loaded per worker: ${workerApi.getIndividualWorkerStatistics map (_.numberOfVertices) mkString (", ")}")
  //    //    println("Worker RPC ...")
  //    //    val individualSystemMemFree = workerApi.getIndividualSystemInformation map (_.jmx_mem_free)
  //    //    println(s"Worker with least amount of free memory: ${((individualSystemMemFree min) / 100000000.0).round / 10.0}GB")
  //    //    println(s"Free memory per worker: ${individualSystemMemFree map { x => ((x / 100000000.0).round / 10.0) + "GB" } mkString(", ")}")
  //    //    verboseIsIdle
  //  }

  protected var workerStatus: Array[WorkerStatus] = new Array[WorkerStatus](numberOfWorkers)
  protected var workerStatusTimestamps: Array[Long] = new Array[Long](numberOfWorkers)
  protected var nodeStatus: Array[NodeStatus] = new Array[NodeStatus](numberOfNodes)

  // Returns true iff the status update was newer than the most recent stored one.
  def handleWorkerStatus(ws: WorkerStatus): Boolean = {
    // A status might be sent indirectly via the node actor, which means that there is no FIFO
    // guarantee. To get FIFO back, we check the time stamp of the status.
    val oldWs = workerStatus(ws.workerId)
    if (oldWs == null || ws.timeStamp > oldWs.timeStamp) {
      updateWorkerStatusMap(ws)
      true
    } else {
      false
    }
  }

  def receive = {
    case BulkStatus(senderNodeId, fromWorkers) =>
      var i = 0
      while (i < fromWorkers.length) {
        handleWorkerStatus(fromWorkers(i))
        i += 1
      }
      if (onIdleList != Nil && isIdle) {
        onIdle
      }
    case ws: WorkerStatus =>
      //log.debug(s"Coordinator received a worker status from worker ${ws.workerId}, the workers idle status is now: ${ws.isIdle}")
      //messageBus.getReceivedMessagesCounter.incrementAndGet
      val wasUpdatePerformed = handleWorkerStatus(ws)
      if (wasUpdatePerformed) {
        if (onIdleList != Nil && isIdle) {
          onIdle
        }
      }
    case OnIdle(action) =>
      onIdleList = (sender, action) :: onIdleList
      if (isIdle) {
        onIdle
      }
    case Request(command, reply, incrementor) =>
      //log.debug(s"Coordinator received a request.")
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
    workerStatus(ws.workerId) = ws
    workerStatusTimestamps(ws.workerId) = System.nanoTime
    if (!allWorkersInitialized) {
      allWorkersInitialized = workerStatus forall (_ != null)
    }
  }

  def updateNodeStatusMap(ns: NodeStatus) {
    nodeStatus(ns.nodeId) = ns
  }

  def onIdle {
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

  def totalMessagesSent = messagesSentToWorkers.sum + messagesSentToNodes.sum + messagesSentToCoordinator
  def totalMessagesReceived = messagesReceivedByWorkers.sum + messagesReceivedByNodes.sum + messagesReceivedByCoordinator

  var messagesSentToCoordinator: Long = 0
  var messagesReceivedByCoordinator: Long = 0
  var messagesSentToWorkers: Array[Long] = new Array(numberOfWorkers)
  var messagesSentToNodes: Array[Long] = new Array(numberOfNodes)
  var messagesReceivedByWorkers: Array[Long] = new Array(numberOfWorkers)
  var messagesReceivedByNodes: Array[Long] = new Array(numberOfNodes)

  def resetMessagingStats {
    messagesSentToCoordinator = 0
    messagesReceivedByCoordinator = 0
    messagesSentToWorkers = new Array(numberOfWorkers)
    messagesSentToNodes = new Array(numberOfNodes)
    messagesReceivedByWorkers = new Array(numberOfWorkers)
    messagesReceivedByNodes = new Array(numberOfNodes)
  }

  def computeMessagingStats {
    def updateSentMessages(stats: SentMessagesStats) {
      messagesSentToCoordinator += stats.coordinator
      for (recipientId <- 0 until numberOfWorkers) {
        messagesSentToWorkers(recipientId) += stats.workers(recipientId)
      }
      for (recipientId <- 0 until numberOfNodes) {
        messagesSentToNodes(recipientId) += stats.nodes(recipientId)
      }
    }
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

  def allSentMessagesReceived: Boolean = {
    resetMessagingStats
    computeMessagingStats
    if (messagesSentToCoordinator != messagesReceivedByCoordinator) {
      //println(s"Coordinator: $messagesReceivedByCoordinator/$messagesSentToCoordinator, globalInbox=${getGlobalInboxSize}")
      return false
    }
    for (nodeId <- 0 until numberOfNodes) {
      if (messagesSentToNodes(nodeId) != messagesReceivedByNodes(nodeId)) {
        //println(s"Node $nodeId: ${messagesReceivedByNodes(nodeId)}/${messagesSentToNodes(nodeId)}, globalInbox=${getGlobalInboxSize}")
        return false
      }
    }
    for (workerId <- 0 until numberOfWorkers) {
      if (messagesSentToWorkers(workerId) != messagesReceivedByWorkers(workerId)) {
        //println(s"Worker $workerId: ${messagesReceivedByWorkers(workerId)}/${messagesSentToWorkers(workerId)}, globalInbox=${getGlobalInboxSize}")
        return false
      }
    }
    true
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
      //log.debug("Coordinator not idle because not all workers have sent a status.")
      return false
    }
    val allIdle = workerStatus.forall(workerStatus => workerStatus.isIdle)
    if (!allIdle) {
      //log.debug("Coordinator not idle because not all workers are idle.")
      return false
    }
    val allSentReceived = allSentMessagesReceived
    if (!allSentReceived) {
      //log.debug("Coordinator not idle because not all sent messages were received.")
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

  def registerWorker(workerId: Int, worker: ActorRef) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerNode(nodeId: Int, node: ActorRef) {
    messageBus.registerNode(nodeId, node)
  }

  def registerCoordinator(coordinator: ActorRef) {
    messageBus.registerCoordinator(coordinator)
  }

}
