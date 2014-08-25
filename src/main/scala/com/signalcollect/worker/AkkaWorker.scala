/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

import java.util.Queue
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import com.signalcollect.Edge
import com.signalcollect.Vertex
import com.signalcollect.interfaces.ActorRestartLogging
import com.signalcollect.interfaces.AddEdge
import com.signalcollect.interfaces.AddVertex
import com.signalcollect.interfaces.BulkSignal
import com.signalcollect.interfaces.BulkSignalNoSourceIds
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.interfaces.ExistingVertexHandlerFactory
import com.signalcollect.interfaces.Heartbeat
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.interfaces.SignalMessageWithSourceId
import com.signalcollect.interfaces.SignalMessageWithoutSourceId
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.UndeliverableSignalHandlerFactory
import com.signalcollect.interfaces.WorkerApi
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Cancellable
import akka.actor.Scheduler
import akka.actor.actorRef2Scala
import akka.dispatch.MessageQueue

case object ScheduleOperations

case object StatsDue

case class StartPingPongExchange(pingPongPartner: Int)
case class Ping(fromWorker: Int)
case class Pong(fromWorker: Int)

/**
 * Incrementor function needs to be defined in its own class to prevent unnecessary
 * closure capture when serialized.
 */
class IncrementorForWorker(workerId: Int) {
  def increment(messageBus: MessageBus[_, _]) = {
    messageBus.incrementMessagesSentToWorker(workerId)
  }
}

/**
 * Class that interfaces the worker implementation with Akka messaging.
 * Mainly responsible for translating received messages to function calls on a worker implementation.
 */
class AkkaWorker[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag](
  val workerId: Int,
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  val messageBusFactory: MessageBusFactory[Id, Signal],
  val mapperFactory: MapperFactory[Id],
  val storageFactory: StorageFactory[Id, Signal],
  val schedulerFactory: SchedulerFactory[Id, Signal],
  val existingVertexHandlerFactory: ExistingVertexHandlerFactory[Id, Signal],
  val undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Id, Signal],
  val edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal],
  val heartbeatIntervalInMilliseconds: Int,
  val eagerIdleDetection: Boolean,
  val throttlingEnabled: Boolean,
  val throttlingDuringLoadingEnabled: Boolean,
  val supportBlockingGraphModificationsInVertex: Boolean)
  extends Actor
  with ActorLogging
  with ActorRestartLogging {

  context.setReceiveTimeout(Duration.Undefined)

  val heartbeatInterval = heartbeatIntervalInMilliseconds * 1000000 // milliseconds to nanoseconds
  var lastHeartbeatTimestamp = System.nanoTime
  var schedulingTimestamp = System.nanoTime

  val akkaScheduler: Scheduler = context.system.scheduler: akka.actor.Scheduler
  implicit val executor = context.system.dispatcher

  override def postStop {
    statsReportScheduling.cancel
    scheduledPingPongExchange.foreach(_.cancel)
    log.debug(s"Worker $workerId has stopped.")
  }

  val statsReportScheduling = akkaScheduler.
    schedule(0.milliseconds, heartbeatIntervalInMilliseconds.milliseconds, self, StatsDue)

  var scheduledPingPongExchange: Option[Cancellable] = None

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    val msg = s"Worker $workerId crashed with ${reason.toString} because of ${reason.getCause} or reason ${reason.getMessage} at position ${reason.getStackTrace.mkString("\n")}, not recoverable."
    println(msg)
    log.error(msg)
    context.stop(self)
  }

  val messageBus: MessageBus[Id, Signal] = messageBusFactory.createInstance(
    context.system,
    numberOfWorkers,
    numberOfNodes,
    mapperFactory.createInstance(numberOfNodes, numberOfWorkers / numberOfNodes),
    new IncrementorForWorker(workerId).increment _)

  val worker = new WorkerImplementation[Id, Signal](
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)

  /**
   * How many graph modifications this worker will execute in one batch.
   */
  val graphModificationBatchProcessingSize = 100

  def isInitialized = messageBus.isInitialized

  def setIdle(newIdleState: Boolean) {
    worker.isIdle = newIdleState
    if (newIdleState == true && eagerIdleDetection && worker.isIdleDetectionEnabled) {
      messageBus.sendToNodeUncounted(worker.nodeId, worker.getWorkerStatusForNode)
    }
    if (numberOfNodes > 1 && !worker.pingPongScheduled && worker.isIdleDetectionEnabled && newIdleState == false) {
      worker.sendPing(worker.getRandomPingPongPartner)
    }
  }

  def applyPendingGraphModifications {
    if (!worker.pendingModifications.isEmpty) {
      try {
        for (modification <- worker.pendingModifications.take(graphModificationBatchProcessingSize)) {
          modification(worker.graphEditor)
        }
      } catch {
        case t: Throwable =>
          println(s"Worker $workerId had a problem during graph loading: $t}")
          t.printStackTrace
      }
      worker.messageBusFlushed = false
    }
  }

  def scheduleOperations {
    setIdle(false)
    self ! ScheduleOperations
    schedulingTimestamp = System.nanoTime
    worker.allWorkDoneWhenContinueSent = worker.isAllWorkDone
    worker.operationsScheduled = true
  }

  val messageQueue: Queue[_] = context.asInstanceOf[{ def mailbox: { def messageQueue: MessageQueue } }].mailbox.messageQueue.asInstanceOf[{ def queue: Queue[_] }].queue

  def handleSignalMessageWithSourceId(s: SignalMessageWithSourceId[Id, Signal]) {
    worker.processSignalWithSourceId(s.signal, s.targetId, s.sourceId)
    if (!worker.operationsScheduled) {
      scheduleOperations
    }
  }

  def handleSignalMessageWithoutSourceId(s: SignalMessageWithoutSourceId[Id, Signal]) {
    worker.processSignalWithoutSourceId(s.signal, s.targetId)
    if (!worker.operationsScheduled) {
      scheduleOperations
    }
  }

  def handleBulkSignalWithSourceIds(bulkSignal: BulkSignal[Id, Signal]) {
    worker.counters.bulkSignalMessagesReceived += 1
    val size = bulkSignal.signals.length
    var i = 0
    while (i < size) {
      val sourceId = bulkSignal.sourceIds(i)
      if (sourceId != null) {
        worker.processSignalWithSourceId(bulkSignal.signals(i), bulkSignal.targetIds(i), sourceId)
      } else {
        worker.processSignalWithoutSourceId(bulkSignal.signals(i), bulkSignal.targetIds(i))
      }
      i += 1
    }
    if (!worker.operationsScheduled) {
      scheduleOperations
    }
  }

  def handleBulkSignalWithoutSourceIds(bulkSignal: BulkSignalNoSourceIds[Id, Signal]) {
    worker.counters.bulkSignalMessagesReceived += 1
    val signals = bulkSignal.signals
    val targetIds = bulkSignal.targetIds
    worker.processBulkSignalWithoutIds(signals, targetIds)
    if (!worker.operationsScheduled) {
      scheduleOperations
    }
  }

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {
    case s: SignalMessageWithSourceId[Id, Signal] =>
      worker.counters.signalMessagesReceived += 1
      handleSignalMessageWithSourceId(s)

    case s: SignalMessageWithoutSourceId[Id, Signal] =>
      worker.counters.signalMessagesReceived += 1
      handleSignalMessageWithoutSourceId(s)

    case bulkSignal: BulkSignal[Id, Signal] =>
      handleBulkSignalWithSourceIds(bulkSignal)

    case bulkSignal: BulkSignalNoSourceIds[Id, Signal] =>
      handleBulkSignalWithoutSourceIds(bulkSignal)

    case StartPingPongExchange(pingPongPartner) =>
      worker.sendPing(pingPongPartner)

    case Ping(fromWorker) =>
      // Answer the ping with a pong.
      messageBus.sendToWorkerUncounted(fromWorker, Pong(workerId))

    case Pong(fromWorker) =>
      worker.waitingForPong = false
      val exchangeDuration = System.nanoTime - worker.pingSentTimestamp
      //val durationInMilliseconds = (exchangeDuration / 1e+4.toDouble).round / 1e+2.toDouble
      //println(s"Exchange between $workerId and $fromWorker took $durationInMilliseconds ms.")
      if (exchangeDuration > worker.maxPongDelay) {
        worker.slowPongDetected = true
        worker.sendPing(worker.getRandomPingPongPartner)
      } else {
        worker.slowPongDetected = false
        // Wait a bit and then play ping pong with another random partner.
        if (!worker.isIdle) {
          scheduledPingPongExchange = Some(akkaScheduler.scheduleOnce(
            worker.pingPongSchedulingIntervalInMilliseconds.milliseconds,
            self,
            StartPingPongExchange(worker.getRandomPingPongPartner)))
        } else {
          worker.pingPongScheduled = false
        }
      }

    case addVertex: AddVertex[Id, _, Id, Signal] =>
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addVertex(addVertex.v)
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case AddEdge(sourceVertexId: Id, edge: Edge[Id]) =>
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addEdge(sourceVertexId, edge)
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case ScheduleOperations =>
      if (messageQueue.isEmpty && System.nanoTime - schedulingTimestamp < 1000000) { // 1 millisecond
        if (worker.allWorkDoneWhenContinueSent && worker.isAllWorkDone) {
          //Worker is now idle.
          setIdle(true)
          worker.operationsScheduled = false
        } else {
          @inline def pongDelayed = worker.waitingForPong && (System.nanoTime - worker.pingSentTimestamp) > worker.maxPongDelay
          val overloaded = worker.slowPongDetected || pongDelayed
          if (worker.isPaused && !(throttlingDuringLoadingEnabled && overloaded)) {
            applyPendingGraphModifications
          } else {
            worker.scheduler.executeOperations(throttlingEnabled && overloaded)
          }
          if (!worker.messageBusFlushed) {
            messageBus.flush
            worker.messageBusFlushed = true
          }
          scheduleOperations
        }
      } else {
        scheduleOperations
      }

    case Request(command, returnResult, incrementorForReply) =>
      worker.counters.requestMessagesReceived += 1
      try {
        val result = command.asInstanceOf[WorkerApi[Id, Signal] => Any](worker)
        if (returnResult) {
          incrementorForReply(messageBus)
          if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException
            messageBus.sendToActor(sender, None)
          } else {
            messageBus.sendToActor(sender, result)
          }
        }
      } catch {
        case t: Throwable =>
          val msg = s"Problematic request on worker $workerId: ${t.getStackTrace.mkString("\n")}"
          println(msg)
          log.debug(msg)
          throw t
      }
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case Heartbeat(unusedFlag) =>
      lastHeartbeatTimestamp = System.nanoTime

    case StatsDue =>
      worker.sendStatusToCoordinator

    case other =>
      worker.counters.otherMessagesReceived += 1
      val msg = s"Worker $workerId could not handle message $other"
      println(msg)
      log.error(msg)
      throw new UnsupportedOperationException(s"Unsupported message: $other")
  }

}
