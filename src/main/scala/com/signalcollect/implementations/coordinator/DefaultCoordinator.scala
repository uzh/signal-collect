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

package com.signalcollect.implementations.coordinator

import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.ExecutionInformation
import com.signalcollect.ExecutionStatistics
import com.signalcollect.GlobalTerminationCondition
import com.signalcollect.configuration.TerminationReason
import akka.actor.Actor
import java.util.concurrent.atomic.AtomicLong
import com.signalcollect.GraphEditor
import akka.actor.ActorRef
import java.util.HashMap
import scala.collection.JavaConversions._
import akka.actor.ReceiveTimeout
import java.util.concurrent.TimeUnit
import akka.util.duration._
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import com.signalcollect.implementations.messaging.Request

// special command for coordinator
case class OnIdle(action: (DefaultCoordinator, ActorRef) => Unit)

// special reply from coordinator
case class IsIdle(b: Boolean)

class DefaultCoordinator(numberOfWorkers: Int, messageBusFactory: MessageBusFactory, maxInboxSize: Option[Long], val loggingLevel: Int) extends Actor with MessageRecipientRegistry with Logging with Coordinator with ActorLogging {

  val messageBus: MessageBus = {
    messageBusFactory.createInstance(numberOfWorkers)
  }

  protected val workerStatusMap = new HashMap[Int, WorkerStatus]()

  def receive = {
    case ws: WorkerStatus =>
      messageBus.getReceivedMessagesCounter.addAndGet(1)
      updateWorkerStatusMap(ws)
      if (isIdle) {
        onIdle
      }
    case OnIdle(action) =>
      // not counting these messages, because they only come from the local graph
      onIdleList = (sender, action) :: onIdleList
      if (isIdle) {
        onIdle
      }
    case Request(command, reply) =>
      try {
        val result = command(this)
        if (reply) {
          sender ! result
        }
      } catch {
        case e: Exception =>
          severe(e)
          throw e
      }
  }

  def updateWorkerStatusMap(ws: WorkerStatus) {
    // only update worker status if no status received so far or if the current status is newer
    if (!workerStatusMap.keySet.contains(ws.workerId) || workerStatusMap.get(ws.workerId).messagesSent < ws.messagesSent) {
      workerStatusMap.put(ws.workerId, ws)
    }
  }

  def onIdle {
    for ((from, action) <- onIdleList) {
      action(this, from)
    }
    onIdleList = List[(ActorRef, (DefaultCoordinator, ActorRef) => Unit)]()
  }

  var waitingStart = System.nanoTime

  var onIdleList = List[(ActorRef, (DefaultCoordinator, ActorRef) => Unit)]()

  protected lazy val workerApi = messageBus.getWorkerApi
  def getWorkerApi = workerApi

  protected lazy val graphEditor = messageBus.getGraphEditor
  def getGraphEditor = graphEditor

  /**
   * The sent worker status messages were not counted yet within that status message, that's why we add config.numberOfWorkers (eventually we will have received at least one status message per worker).
   *
   * Initialization messages sent to the workers do not have to be taken into account, because they are balanced by replies from the workers that do not get counted when they are received.
   */
  def messagesSentByWorkers = workerStatusMap.values.foldLeft(0l)(_ + _.messagesSent) + numberOfWorkers // 
  def messagesSentByCoordinator = messageBus.messagesSent

  def messagesReceivedByWorkers = workerStatusMap.values.foldLeft(0l)(_ + _.messagesReceived)
  def messagesReceivedByCoordinator = messageBus.messagesReceived

  def totalMessagesSent: Long = messagesSentByWorkers + messagesSentByCoordinator
  def totalMessagesReceived: Long = messagesReceivedByWorkers + messagesReceivedByCoordinator
  def globalInboxSize: Long = totalMessagesSent - totalMessagesReceived
  
  def isOverstrained: Boolean = {
    if (!maxInboxSize.isDefined) {
      false
    } else {
      globalInboxSize > maxInboxSize.get
    }
  }

  def isIdle: Boolean = {
    debug("""
        Is idle stats:
    		Workers that are not idle yet: """ + workerStatusMap.values.filter(!_.isIdle).foldLeft("")(_ + _) + """
    		totalMessagesSent: """ + totalMessagesSent + """ totalMessagesReceived: """ + totalMessagesReceived + """
    		coordinatorMessagesSent: """ + messageBus.getSentMessagesCounter.get + """ coordinatorMessagesReceived: """ + messageBus.getReceivedMessagesCounter.get + """
    		workerStatusMaps: """ + workerStatusMap.values)
    workerStatusMap.values.forall(_.isIdle) && totalMessagesSent == totalMessagesReceived
  }

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

  def registerCoordinator(coordinator: ActorRef) {
    messageBus.registerCoordinator(coordinator)
  }

  def registerLogger(logger: ActorRef) {
    messageBus.registerLogger(logger)
  }

}