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

import scala.concurrent.duration._
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Queue
import java.lang.management.ManagementFactory
import scala.Array.canBuildFrom
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import com.signalcollect._
import com.signalcollect.interfaces._
import com.sun.management.OperatingSystemMXBean
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout
import akka.dispatch.MessageQueue
import akka.actor.Actor
import akka.serialization.SerializationExtension
import akka.actor.Cancellable
import akka.actor.Scheduler
import scala.util.Random
import com.signalcollect.coordinator.IsIdle

case object ScheduleOperations

case object StatsDue

case class StartPingPongExchange(pingPongPartner: Int)
case class Ping(fromWorker: Int)
case class Pong(fromWorker: Int)

/**
 * Incrementor function needs to be defined in its own class to prevent unnecessary
 * closure capture when serialized.
 */
case class IncrementorForWorker(workerId: Int) {
  def increment(messageBus: MessageBus[_, _]) = {
    messageBus.incrementMessagesSentToWorker(workerId)
  }
}

/**
 * Class that interfaces the worker implementation with Akka messaging.
 * Mainly responsible for translating received messages to function calls on a worker implementation.
 */
class AkkaWorker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val workerId: Int,
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  val messageBusFactory: MessageBusFactory,
  val mapperFactory: MapperFactory,
  val storageFactory: StorageFactory,
  val schedulerFactory: SchedulerFactory,
  val heartbeatIntervalInMilliseconds: Int,
  val eagerIdleDetection: Boolean,
  val throttlingEnabled: Boolean)
  extends WorkerActor[Id, Signal]
  with ActorLogging
  with ActorRestartLogging {

  val heartbeatInterval = heartbeatIntervalInMilliseconds * 1000000 // milliseconds to nanoseconds
  var lastHeartbeatTimestamp = System.nanoTime
  var schedulingTimestamp = System.nanoTime

  override def postStop {
    statsReportScheduling.cancel
    scheduledPingPongExchange.foreach(_.cancel)
    log.debug(s"Worker $workerId has stopped.")
  }

  // Assumes that there is the same number of workers on all nodes.
  def nodeId: Int = {
    val workersPerNode = numberOfWorkers / numberOfNodes
    workerId / workersPerNode
  }

  implicit val executor = context.system.dispatcher
  val statsReportScheduling = context.system.scheduler.
    schedule(0.milliseconds, heartbeatIntervalInMilliseconds.milliseconds, self, StatsDue)

  var scheduledPingPongExchange: Option[Cancellable] = None

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    val msg = s"Worker $workerId crashed with ${reason.toString} because of ${reason.getCause} or reason ${reason.getMessage} at position ${reason.getStackTrace.mkString("\n")}, not recoverable."
    println(msg)
    log.error(msg)
    context.stop(self)
  }

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](
      context.system,
      numberOfWorkers,
      numberOfNodes,
      mapperFactory.createInstance(numberOfNodes, numberOfWorkers / numberOfNodes),
      IncrementorForWorker(workerId).increment _)
  }

  val worker = new WorkerImplementation[Id, Signal](
    workerId = workerId,
    nodeId = nodeId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0,
    existingVertexHandler = (vOld, vNew, ge) => (),
    undeliverableSignalHandler = (s: Signal, tId: Id, sId: Option[Id], ge: GraphEditor[Id, Signal]) => {
      throw new Exception(s"Undeliverable signal: $s from $sId could not be delivered to $tId.")
      Unit
    },
    edgeAddedToNonExistentVertexHandler = (edge: Edge[Id], vertexId: Id) => {
      throw new Exception(
        s"Could not add edge: ${edge.getClass.getSimpleName}(id = $vertexId -> ${edge.targetId}), because vertex with id $vertexId does not exist.")
      None
    })

  /**
   * How many graph modifications this worker will execute in one batch.
   */
  val graphModificationBatchProcessingSize = 100

  def isInitialized = messageBus.isInitialized

  def setIdle(newIdleState: Boolean) {
    worker.isIdle = newIdleState
    if (newIdleState == true && eagerIdleDetection && worker.isIdleDetectionEnabled) {
      messageBus.sendToNodeUncounted(nodeId, worker.getWorkerStatusForNode)
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

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {
    case s: SignalMessage[Id, Signal] =>
      worker.counters.signalMessagesReceived += 1
      worker.processSignal(s.signal, s.targetId, s.sourceId)
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case bulkSignal: BulkSignal[Id, Signal] =>
      worker.counters.bulkSignalMessagesReceived += 1
      val size = bulkSignal.signals.length
      var i = 0
      while (i < size) {
        val sourceId = bulkSignal.sourceIds(i)
        if (sourceId != null) {
          worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), Some(sourceId))
        } else {
          worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
        }
        i += 1
      }
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case bulkSignal: BulkSignalNoSourceIds[Id, Signal] =>
      worker.counters.bulkSignalMessagesReceived += 1
      val size = bulkSignal.signals.length
      var i = 0
      while (i < size) {
        worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
        i += 1
      }
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

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
        // Immediately play ping-pong with the same slow partner again.
        worker.pingSentTimestamp = System.nanoTime
        messageBus.sendToWorkerUncounted(fromWorker, Ping(workerId))
      } else {
        worker.slowPongDetected = false
        // Wait a bit and then play ping pong with another random partner.
        if (!worker.isIdle) {
          scheduledPingPongExchange = Some(context.system.scheduler.scheduleOnce(
            worker.pingPongSchedulingIntervalInMilliseconds.milliseconds,
            self,
            StartPingPongExchange(worker.getRandomPingPongPartner)))
        } else {
          worker.pingPongScheduled = false
        }
      }

    case AddVertex(vertex) =>
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addVertex(vertex.asInstanceOf[Vertex[Id, _]])
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case AddEdge(sourceVertexId, edge) =>
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addEdge(sourceVertexId.asInstanceOf[Id], edge.asInstanceOf[Edge[Id]])
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled) {
        scheduleOperations
      }

    case ScheduleOperations =>
      if (messageQueue.isEmpty && System.nanoTime - schedulingTimestamp < 1000000) { // 1 millisecond
        //        log.debug(s"Message queue on worker $workerId is empty")
        if (worker.allWorkDoneWhenContinueSent && worker.isAllWorkDone) {
          //          log.debug(s"Worker $workerId turns to idle")

          //Worker is now idle.
          setIdle(true)
          worker.operationsScheduled = false
        } else {
          //def timeSinceLastHeartbeat = System.nanoTime - lastHeartbeatTimestamp
          def pongDelayed = worker.waitingForPong && (System.nanoTime - worker.pingSentTimestamp) > worker.maxPongDelay
          //def heartbeatDelayed = timeSinceLastHeartbeat > heartbeatInterval   heartbeatDelayed ||  || worker.systemOverloaded
          val overloaded = throttlingEnabled && (
            pongDelayed || worker.slowPongDetected)
          //          log.debug(s"Worker $workerId has work to do")
          if (worker.isPaused) { // && !overloaded
            //            log.debug(s"Worker $workerId is paused. Pending worker operations: ${!worker.pendingModifications.isEmpty}")
            applyPendingGraphModifications
          } else {
            //            log.debug(s"Worker $workerId is not paused. Will execute operations.")
            worker.scheduler.executeOperations(overloaded)
          }
          if (!worker.messageBusFlushed) {
            messageBus.flush
            worker.messageBusFlushed = true
          }
          //          log.debug(s"Worker $workerId will schedule operations.")
          scheduleOperations
        }
      } else {
        //        log.debug(s"Message queue on worker $workerId is NOT empty, will schedule operations")
        scheduleOperations
      }

    case Request(command, reply, incrementor) =>
      worker.counters.requestMessagesReceived += 1
      try {
        val result = command.asInstanceOf[WorkerApi[Id, Signal] => Any](worker)
        if (reply) {
          incrementor(messageBus)
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

    case Heartbeat(maySignal) =>
      lastHeartbeatTimestamp = System.nanoTime
    //worker.systemOverloaded = !maySignal

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
