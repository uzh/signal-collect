/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.messaging

import com.signalcollect.interfaces._
import java.util.HashMap
import com.signalcollect.logging.DefaultLogger
import com.signalcollect.interfaces.LogMessage
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorRef
import com.signalcollect.coordinator.WorkerApi
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.Edge
import java.util.Random

trait AbstractMessageBus[@specialized(Int, Long) Id, @specialized(Int, Long, Float, Double) Signal] extends MessageBus[Id, Signal] with GraphEditor[Id, Signal] {

  def numberOfWorkers: Int

  protected var registrations = new AtomicInteger()

  def flush = {}

  def isInitialized = registrations.get == numberOfWorkers + 2

  protected val mapper: VertexToWorkerMapper[Id] = new DefaultVertexToWorkerMapper[Id](numberOfWorkers)

  protected val workers = new Array[ActorRef](numberOfWorkers)

  protected val workerIds = (0 until numberOfWorkers).toList

  protected var logger: ActorRef = _

  protected var coordinator: ActorRef = _

  val coordinatorId = Coordinator.getCoodinatorPosition(numberOfWorkers)
  val otherRecipients = Coordinator.getOthersPosition(numberOfWorkers)

  protected var sentMessagesCounters: Array[AtomicInteger] = {
    val counters = new Array[AtomicInteger](numberOfWorkers + 2)
    var i = 0
    while (i < counters.length) {
      counters(i) = new AtomicInteger(0)
      i += 1
    }
    counters
  }

  protected val receivedMessagesCounter = new AtomicInteger(0)
  def getReceivedMessagesCounter: AtomicInteger = receivedMessagesCounter

  lazy val parallelWorkers = workers.par

  lazy val workerProxies: Array[Worker[Id, Signal]] = {
    val result = new Array[Worker[Id, Signal]](numberOfWorkers)
    for (workerId <- workerIds) {
      result(workerId) = AkkaProxy.newInstance[Worker[Id, Signal]](workers(workerId), sentMessagesCounters(workerId), receivedMessagesCounter)
    }
    result
  }

  lazy val workerApi = new WorkerApi(workerProxies, mapper)

  /**
   * Creates a copy of the message counters map and transforms the values from AtomicInteger to Long type.
   */
  def messagesSent = sentMessagesCounters.map((c: AtomicInteger) => c.get)

  def messagesReceived = receivedMessagesCounter.get

  //--------------------MessageRecipientRegistry--------------------

  def registerWorker(workerId: Int, worker: ActorRef) {
    workers(workerId) = worker
    registrations.incrementAndGet
  }

  def registerCoordinator(c: ActorRef) {
    coordinator = c
    registrations.incrementAndGet
  }

  def registerLogger(l: ActorRef) {
    logger = l
    registrations.incrementAndGet
  }

  //--------------------MessageBus--------------------

  def sendToActor(actor: ActorRef, message: Any) {
    sentMessagesCounters(otherRecipients).incrementAndGet()
    actor ! message
  }

  def sendToCoordinator(message: Any) {
    sentMessagesCounters(coordinatorId).incrementAndGet
    coordinator ! message
  }

  def sendToLogger(message: LogMessage) {
    if (logger != null) {
      logger ! message
    } else {
      println("Could not log message: " + message)
    }
  }

  def sendToWorkerForVertexId(message: Any, recipientId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(recipientId)
    sendToWorker(workerId, message)
  }

  def sendToWorkerForVertexIdHash(message: Any, recipientIdHash: Int) {
    val workerId = mapper.getWorkerIdForVertexIdHash(recipientIdHash)
    sendToWorker(workerId, message)
  }

  def sendToWorker(workerId: Int, message: Any) {
    sentMessagesCounters(workerId).incrementAndGet
    workers(workerId) ! message
  }

  def sendToWorkers(message: Any, messageCounting: Boolean) {
    if (messageCounting) {
      workerIds.foreach(sentMessagesCounters(_).incrementAndGet)
    }
    for (worker <- parallelWorkers) {
      worker ! message
    }
  }

  def getWorkerIdForVertexId(vertexId: Id): Int = mapper.getWorkerIdForVertexId(vertexId)

  def getWorkerIdForVertexIdHash(vertexIdHash: Int): Int = mapper.getWorkerIdForVertexIdHash(vertexIdHash)

  //--------------------GraphEditor--------------------

  /**
   * Sends a signal to the vertex with vertex.id=edgeId.targetId
   */
  def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.sendSignal(signal, targetId, sourceId)
    } else {
      // Manually send a fire & forget request.
      sendToWorkerForVertexId(SignalMessage(targetId, sourceId, signal), targetId)
    }
  }

  def addVertex(vertex: Vertex[Id, _], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.addVertex(vertex)
    } else {
      // Manually send a fire & forget request.
      val request = Request[Worker[Id, Signal]]((_.addVertex(vertex)), returnResult = false)
      sendToWorkerForVertexId(request, vertex.id)
    }
  }

  def addEdge(sourceId: Id, edge: Edge[Id], blocking: Boolean = false) {
    // thread that uses an object should instantiate it (performance)
    if (blocking) {
      // use proxy
      workerApi.addEdge(sourceId, edge)
    } else {
      // Manually send a fire & forget request.
      val request = Request[Worker[Id, Signal]]((_.addEdge(sourceId, edge)), returnResult = false)
      sendToWorkerForVertexId(request, sourceId)
    }
  }

  def removeVertex(vertexId: Id, blocking: Boolean = false) {
    if (blocking) {
      // use proxy
      workerApi.removeVertex(vertexId)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker[Id, Signal]]((_.removeVertex(vertexId)), returnResult = false)
      sendToWorkerForVertexId(request, vertexId)
    }
  }

  def removeEdge(edgeId: EdgeId[Id], blocking: Boolean = false) {
    if (blocking) {
      // use proxy
      workerApi.removeEdge(edgeId)
    } else {
      // manually send a fire & forget request     
      val request = Request[Worker[Id, Signal]]((_.removeEdge(edgeId)), returnResult = false)
      sendToWorkerForVertexId(request, edgeId.sourceId)
    }
  }

  def loadGraph(vertexIdHint: Option[Id] = None, graphLoader: GraphEditor[Id, Signal] => Unit, blocking: Boolean = false) {
    if (blocking) {
      workerApi.loadGraph(vertexIdHint, graphLoader)
    } else {
      val request = Request[Worker[Id, Signal]]((_.loadGraph(graphLoader)), returnResult = false)
      if (vertexIdHint.isDefined) {
        val workerId = vertexIdHint.get.hashCode % workers.length
        sendToWorker(workerId, request)
      } else {
        val rand = new Random
        sendToWorker(rand.nextInt(workers.length), request)
      }
    }

  }

  //--------------------Access to high-level messaging constructs--------------------

  def getGraphEditor: GraphEditor[Id, Signal] = this

  def getWorkerApi: WorkerApi[Id, Signal] = workerApi

  def getWorkerProxies: Array[Worker[Id, Signal]] = workerProxies
}