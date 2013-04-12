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
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import com.signalcollect._
import com.signalcollect.interfaces._
import com.signalcollect.serialization.DefaultSerializer
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout
import akka.dispatch.MessageQueue
import akka.actor.Actor

object ScheduleOperations

/**
 * Class that interfaces the worker implementation with Akka messaging.
 * Mainly responsible for translating received messages to function calls on a worker implementation.
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

  val worker = new WorkerImplementation[Id, Signal](
    workerId = workerId,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    signalThreshold = 0.001,
    collectThreshold = 0.0,
    undeliverableSignalHandler = (s: Signal, tId: Id, sId: Option[Id], ge: GraphEditor[Id, Signal]) => {
      throw new Exception(s"Undeliverable signal: $s from $sId could not be delivered to $tId")
      Unit
    })

  /**
   * How many graph modifications this worker will execute in one batch.
   */
  val graphModificationBatchProcessingSize = 100

  def isInitialized = messageBus.isInitialized

  def applyPendingGraphModifications {
    if (worker.pendingModifications.hasNext) {
      for (modification <- worker.pendingModifications.take(graphModificationBatchProcessingSize)) {
        modification(worker.graphEditor)
      }
      if (worker.pendingModifications.hasNext) {
        scheduleOperations
      }
    }
  }

  def scheduleOperations {
    worker.setIdle(false)
    self ! ScheduleOperations
    worker.allWorkDoneWhenContinueSent = worker.isAllWorkDone
  }

  val messageQueue: Queue[_] = context.asInstanceOf[{ def mailbox: { def messageQueue: MessageQueue } }].mailbox.messageQueue.asInstanceOf[{ def queue: Queue[_] }].queue

  
  def executeOperations {
    if (messageQueue.isEmpty) {
      val collected = worker.vertexStore.toCollect.process(
        vertex => {
          worker.executeCollectOperationOfVertex(vertex, addToSignal = false)
          if (vertex.scoreSignal > worker.signalThreshold) {
            worker.executeSignalOperationOfVertex(vertex)
          }
        })
//        log.debug(s"Worker $workerId is signaling, ${worker.vertexStore.toSignal.size} signal operations to go")
      if (!worker.vertexStore.toSignal.isEmpty && messageQueue.isEmpty) {
        worker.vertexStore.toSignal.process(worker.executeSignalOperationOfVertex(_))
      }
//      log.debug(s"Worker $workerId has ${worker.vertexStore.toSignal.size} vertices in toSignal left")
      messageBus.flush
    }
  }

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {
    case s: SignalMessage[Id, Signal] =>
      log.debug(s"$workerId $s")
      worker.counters.signalMessagesReceived += 1
      worker.processSignal(s.signal, s.targetId, s.sourceId)
      if (!worker.operationsScheduled && !worker.isPaused) {
        scheduleOperations
        worker.operationsScheduled = true
      }

    case bulkSignal: BulkSignal[Id, Signal] =>
      log.debug(s"$workerId $bulkSignal")
      worker.counters.bulkSignalMessagesReceived += 1
      val size = bulkSignal.signals.length
      var i = 0
      if (bulkSignal.sourceIds != null) {
        while (i < size) {
          val sourceId = bulkSignal.sourceIds(i)
          if (sourceId != null) {
            worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), Some(sourceId))
          } else {
            worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
          }
          i += 1
        }
      } else {
        while (i < size) {
          worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
          i += 1
        }
      }
      if (!worker.operationsScheduled && !worker.isPaused) {
        scheduleOperations
        worker.operationsScheduled = true
      }

    case ScheduleOperations =>
      log.debug(s"$workerId ScheduleOperations")
      if (worker.allWorkDoneWhenContinueSent && worker.isAllWorkDone) {
        worker.setIdle(true)
        worker.operationsScheduled = false
      } else {
        executeOperations
        scheduleOperations
      }

    case Request(command, reply) =>
      log.debug(s"$workerId $command")
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
          log.error(s"Problematic request on worker $workerId: ${e.getMessage}")
          throw e
      }
      if (!worker.operationsScheduled && (!worker.isAllWorkDone || !worker.isIdle)) {
        scheduleOperations
        worker.operationsScheduled = true
      }

    case Heartbeat(maySignal) =>
      log.debug(s"$workerId Heartbeat(maySignal=$maySignal) ${worker.getWorkerStatus}")
      worker.counters.heartbeatMessagesReceived += 1
      worker.sendStatusToCoordinator
      worker.systemOverloaded = !maySignal

    case other =>
      log.debug(s"$workerId Other: $other")
      worker.counters.otherMessagesReceived += 1
      log.error(s"Worker $workerId not handle message $other")
      throw new Exception(s"Unsupported message: $other")
  }

}