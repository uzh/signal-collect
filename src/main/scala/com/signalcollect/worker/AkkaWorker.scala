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
import com.signalcollect.serialization.DefaultSerializer
import com.sun.management.OperatingSystemMXBean
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout
import akka.dispatch.MessageQueue
import akka.actor.Actor
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import scala.sys.process._

case object ReportStatusToCoordinator

case class StartPingPongExchange(pingPongPartner: Int)

case class Ping(fromWorker: Int)
case class Pong(fromWorker: Int)

case class ScheduleOperations(timestamp: Long)

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
  val reportingIntervalInMilliseconds: Int,
  val pingPongSchedulingIntervalInMilliseconds: Int = 100)
  extends WorkerActor[Id, Signal]
  with ActorLogging
  with ActorRestartLogging {

  def getRandomPingPongPartner = Random.nextInt(numberOfWorkers)
  var pingSentTimestamp: Long = _

  context.system.scheduler.schedule(reportingIntervalInMilliseconds.milliseconds, reportingIntervalInMilliseconds.milliseconds, self, ReportStatusToCoordinator)

  override def postStop {
    log.debug(s"Worker $workerId has stopped.")
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    val msg = s"Worker $workerId crashed with ${reason.toString} because of ${reason.getCause} or reason ${reason.getMessage} at position ${reason.getStackTraceString}, not recoverable."
    //println(msg)
    log.error(msg)
    context.stop(self)
  }

  val messageBus: MessageBus[Id, Signal] = {
    messageBusFactory.createInstance[Id, Signal](
      numberOfWorkers,
      numberOfNodes,
      mapperFactory.createInstance(numberOfNodes, numberOfWorkers / numberOfNodes),
      IncrementorForWorker(workerId).increment _)
  }

  val worker = new WorkerImplementation[Id, Signal](
    workerId = workerId,
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

  def applyPendingGraphModifications {
    if (!worker.pendingModifications.isEmpty) {
      try {
        for (modification <- worker.pendingModifications.take(graphModificationBatchProcessingSize)) {
          modification(worker.graphEditor)
        }
      } catch {
        case t: Throwable =>
          println(s"Worker $workerId had a problem during graph loading: ${t.toString}")
          println(t.getStackTrace.mkString("\n"))
      }
      worker.messageBusFlushed = false
    }
  }

  def scheduleOperations {
    self ! ScheduleOperations(System.nanoTime)
    worker.allWorkDoneWhenContinueSent = worker.isAllWorkDone
    worker.operationsScheduled = true
  }

  //val messageQueue: Queue[_] = context.asInstanceOf[{ def mailbox: { def messageQueue: MessageQueue } }].mailbox.messageQueue.asInstanceOf[{ def queue: Queue[_] }].queue

  /**
   * This method gets executed when the Akka actor receives a message.
   */
  def receive = {
    case s: SignalMessage[Id, Signal] =>
      //println(s"$workerId Id received a signal")
      worker.counters.signalMessagesReceived += 1
      worker.processSignal(s.signal, s.targetId, s.sourceId)
      if (!worker.operationsScheduled && !worker.isPaused) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    case bulkSignal: BulkSignal[Id, Signal] =>
      //println(s"$workerId Id received a bulk signal")
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
      if (!worker.operationsScheduled && !worker.isPaused) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    case bulkSignal: BulkSignalNoSourceIds[Id, Signal] =>
      //println(s"$workerId Id received a bulk signal without source ids")
      worker.counters.bulkSignalMessagesReceived += 1
      val size = bulkSignal.signals.length
      var i = 0
      while (i < size) {
        worker.processSignal(bulkSignal.signals(i), bulkSignal.targetIds(i), None)
        i += 1
      }
      if (!worker.operationsScheduled && !worker.isPaused) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    case AddVertex(vertex) =>
      //println(s"$workerId Id received an add vertex message")
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addVertex(vertex.asInstanceOf[Vertex[Id, _]])
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled && !worker.isAllWorkDone) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    case AddEdge(sourceVertexId, edge) =>
      //println(s"$workerId Id received an add edge message")
      // TODO: More precise accounting for this kind of message.
      worker.counters.requestMessagesReceived += 1
      worker.addEdge(sourceVertexId.asInstanceOf[Id], edge.asInstanceOf[Edge[Id]])
      // TODO: Reevaluate, if we really need to schedule operations.
      if (!worker.operationsScheduled && !worker.isAllWorkDone) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    case ReportStatusToCoordinator =>
      //println(s"$workerId was told to report its status to the coordinator")
      if (messageBus.isInitialized) {
        worker.reportStatusToCoordinator
      }

    case ScheduleOperations(timestamp) =>
      //println(s"$workerId was told to schedule operations")
      if (worker.allWorkDoneWhenContinueSent && worker.isAllWorkDone) {
        worker.operationsScheduled = false
        //worker.reportStatusToCoordinator
      } else {
        val largeInboxSize = System.nanoTime - timestamp > 100000000 // 100 milliseconds
        if (largeInboxSize) {
          def bytesToGigabytes(bytes: Long) = ((bytes / 1073741824.0) * 10.0).round / 10.0
          val free = bytesToGigabytes(Runtime.getRuntime.freeMemory).toString + "GB"
          val freeSystem = "free -m" !!
          
          println(s"$workerId has a large inbox size, free memory = $free, on system: $freeSystem")
        }
        if (worker.isPaused) {
          if (!largeInboxSize && !worker.systemOverloaded) {
            applyPendingGraphModifications
          }
          //          else {
          //            println(s"$workerId is paused and either has a large inbox or the system is overloaded")
          //          }
        } else {
          if (!largeInboxSize && !worker.systemOverloaded) {
            worker.scheduler.executeOperations(largeInboxSize || worker.systemOverloaded)
          } else {
            //println(s"$workerId is not paused and has a large inbox ($largeInboxSize) or the system is overloaded (${worker.systemOverloaded})")
            worker.scheduler.executeOperations(largeInboxSize || worker.systemOverloaded)
          }
        }
        if (!worker.messageBusFlushed) {
          messageBus.flush
          worker.messageBusFlushed = true
        }
        scheduleOperations
      }

    case Request(command, reply, incrementor) =>
      //println(s"$workerId got a request")
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
          val msg = s"Problematic request on worker $workerId: ${t.getStackTraceString}"
          println(msg)
          log.debug(msg)
          throw t
      }
      if (pingSentTimestamp == 0 && messageBus.isInitialized) {
        pingSentTimestamp = 1
        context.system.scheduler.scheduleOnce(pingPongSchedulingIntervalInMilliseconds.milliseconds, self, StartPingPongExchange(getRandomPingPongPartner))
      }
      if (!worker.operationsScheduled && !worker.isAllWorkDone) {
        scheduleOperations
      } else {
        //worker.reportStatusToCoordinator
      }

    //    case Heartbeat(maySignal) =>
    //      worker.counters.heartbeatMessagesReceived += 1
    //      worker.systemOverloaded = !maySignal

    case s @ StartPingPongExchange(partner) =>
      //println(s"$workerId was told to start a pingpong exchange")
      if (messageBus.isInitialized) {
        pingSentTimestamp = System.nanoTime
        //println(s"Ping($workerId) -> $partner")
        messageBus.sendToWorkerUncounted(partner, Ping(workerId))
      } else {
        context.system.scheduler.scheduleOnce(pingPongSchedulingIntervalInMilliseconds.milliseconds, self, s)
      }

    case p @ Ping(fromWorker) =>
      //println(s"$workerId got a ping from $fromWorker")
      //println(s"Pong($workerId) -> $fromWorker")
      if (messageBus.isInitialized) {
        messageBus.sendToWorkerUncounted(fromWorker, Pong(workerId))
      } else {
        // Schedule message for later.
        context.system.scheduler.scheduleOnce(pingPongSchedulingIntervalInMilliseconds.milliseconds, self, p)
      }

    case Pong(fromWorker) =>
      //println(s"$workerId got a pong from $fromWorker")
      val exchangeDuration = System.nanoTime - pingSentTimestamp
      if (System.nanoTime - pingSentTimestamp > 50000000) { // 50 milliseconds
        println(s"Exchange between $workerId and $fromWorker took too long.")
        worker.systemOverloaded = true
        pingSentTimestamp = System.nanoTime
        // Immediately play ping-pong with the same slow partner again.
        //println(s"Ping($workerId) -> $fromWorker")
        messageBus.sendToWorkerUncounted(fromWorker, Ping(workerId))
      } else {
        worker.systemOverloaded = false
        // Wait a bit and then play ping pong with another random partner.
        context.system.scheduler.scheduleOnce(pingPongSchedulingIntervalInMilliseconds.milliseconds, self, StartPingPongExchange(getRandomPingPongPartner))
      }

    case other =>
      //println(s"$workerId some other message $other")
      worker.counters.otherMessagesReceived += 1
      val msg = s"Worker $workerId could not handle message $other"
      println(msg)
      log.error(msg)
      throw new UnsupportedOperationException(s"Unsupported message: $other")
  }

}
