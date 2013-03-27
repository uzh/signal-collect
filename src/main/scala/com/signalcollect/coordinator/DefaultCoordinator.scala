/*
 *  @author Philip Stutz
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
import java.util.HashMap
import java.util.Map
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.reflect.ClassTag
import com.signalcollect.interfaces._
import com.sun.management.OperatingSystemMXBean
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout
import akka.actor.actorRef2Scala
import akka.event.Logging.LogLevel
import akka.event.Logging
import com.signalcollect.messaging.AkkaProxy

// special command for coordinator
case class OnIdle(action: (DefaultCoordinator[_, _], ActorRef) => Unit)

// special reply from coordinator
case class IsIdle(b: Boolean)

class DefaultCoordinator[Id: ClassTag, Signal: ClassTag](
  numberOfWorkers: Int,
  messageBusFactory: MessageBusFactory,
  loggerRef: ActorRef,
  heartbeatIntervalInMilliseconds: Long)
  extends Actor with MessageRecipientRegistry
  with Coordinator[Id, Signal]
  with ActorLogging {

  /**
   * Timeout for Akka actor idling
   */
  context.setReceiveTimeout(Duration.Undefined)

  val logger = AkkaProxy.newInstance[Logger](loggerRef)
  
  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](
      numberOfWorkers)
  }

  val heartbeatInterval = heartbeatIntervalInMilliseconds * 1000000 // milliseconds to nanoseconds
  var lastHeartbeatTimestamp = 0l

  def shouldSendHeartbeat: Boolean = {
    (System.nanoTime - lastHeartbeatTimestamp) > heartbeatInterval
  }

  var globalQueueSizeLimitPreviousHeartbeat = 0l
  var globalReceivedMessagesPreviousHeartbeat = 0l

  def sendHeartbeat {
    log.debug("idle: " + workerStatus.filter(workerStatus => workerStatus != null && workerStatus.isIdle).size + "/" + numberOfWorkers + ", global inbox: " + getGlobalInboxSize)
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
    globalReceivedMessagesPreviousHeartbeat = currentMessagesReceived
    globalQueueSizeLimitPreviousHeartbeat = currentGlobalQueueSize
  }

  protected var workerStatus: Array[WorkerStatus] = new Array[WorkerStatus](numberOfWorkers)
  def getWorkerStatus(): Array[WorkerStatus] = { return workerStatus }

  def receive = {
    case ws: WorkerStatus =>
      messageBus.getReceivedMessagesCounter.incrementAndGet
      updateWorkerStatusMap(ws)
      if (isIdle) {
        onIdle
      }
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case ReceiveTimeout =>
      if (shouldSendHeartbeat) {
        sendHeartbeat
      }
    case OnIdle(action) =>
      context.setReceiveTimeout(heartbeatIntervalInMilliseconds.milliseconds)
      // Not counting these messages, because they only come from the local graph.
      onIdleList = (sender, action) :: onIdleList
      if (isIdle) {
        onIdle
      }
    case Request(command, reply) =>
      try {
        val result = command.asInstanceOf[Coordinator[Id, Signal] => Any](this)
        if (reply) {
          sender ! result
        }
      } catch {
        case t: Throwable =>
          log.error(t.toString)
          throw t
      }
  }

  def updateWorkerStatusMap(ws: WorkerStatus) {
    // Only update worker status if no status received so far or if the current status is newer.
    if (workerStatus(ws.workerId) == null || workerStatus(ws.workerId).messagesSent.sum < ws.messagesSent.sum) {
      workerStatus(ws.workerId) = ws
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

  /**
   * The sent worker status messages were not counted yet within that status message, that's why we add config.numberOfWorkers (eventually we will have received at least one status message per worker).
   *
   * Initialization messages sent to the workers have to be added separately.
   *
   * + numberOfWorkers, because the status message sent by the worker is not included in the stats yet.
   */
  def messagesSentByWorkers: Long = messagesSentPerWorker.values.sum + numberOfWorkers + initializationMessages

  val initializationMessages = numberOfWorkers * (numberOfWorkers + 2) // +2 for registration of coordinator and logger

  /**
   *  Returns a map with the worker id as the key and the number of messages sent as the value.
   */
  def messagesSentPerWorker: Map[Int, Long] = {
    val messagesPerWorker = new HashMap[Int, Long]()
    var workerId = 0
    while (workerId < numberOfWorkers) {
      val status = workerStatus(workerId)
      messagesPerWorker.put(workerId, if (status == null) 0 else status.messagesSent.sum)
      workerId += 1
    }
    messagesPerWorker
  }

  def messagesSentByCoordinator = messageBus.messagesSent.sum
  def messagesReceivedByWorkers = workerStatus filter (_ != null) map (_.messagesReceived) sum
  def messagesReceivedByCoordinator = messageBus.messagesReceived
  def totalMessagesSent: Long = messagesSentByWorkers + messagesSentByCoordinator
  def totalMessagesReceived: Long = messagesReceivedByWorkers + messagesReceivedByCoordinator
  def getGlobalInboxSize: Long = totalMessagesSent - totalMessagesReceived

  def isIdle: Boolean = {
    workerStatus.forall(workerStatus => workerStatus != null && workerStatus.isIdle) && totalMessagesSent == totalMessagesReceived
  }

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }
  
  def getLogMessages(logLevel: LogLevel, numberOfMessages: Int) = {
    logger.getLogMessages(logLevel, numberOfMessages)
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
}
