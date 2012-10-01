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
import com.signalcollect.serialization.DefaultSerializer
import java.util.Random
import com.signalcollect.serialization.DefaultSerializer

class DefaultMessageBus(
  val numberOfWorkers: Int)
  extends MessageBus with GraphEditor with DefaultSerializer {

  protected var registrations = new AtomicInteger()

  def isInitialized = registrations.get == numberOfWorkers + 2

  protected val mapper: VertexToWorkerMapper = new DefaultVertexToWorkerMapper(numberOfWorkers)

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

  lazy val workerProxies: Array[Worker] = {
    val result = new Array[Worker](numberOfWorkers)
    for (workerId <- workerIds) {
      result(workerId) = AkkaProxy.newInstance[Worker](workers(workerId), sentMessagesCounters(workerId), receivedMessagesCounter)
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

  def sendToWorkerForVertexId(message: Any, recipientId: Any) {
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

  def sendToWorkers(message: Any) {
    workerIds.foreach(sentMessagesCounters(_).incrementAndGet)
    for (worker <- parallelWorkers) {
      worker ! message
    }
  }

  def getWorkerIdForVertexId(vertexId: Any): Int = mapper.getWorkerIdForVertexId(vertexId)

  def getWorkerIdForVertexIdHash(vertexIdHash: Int): Int = mapper.getWorkerIdForVertexIdHash(vertexIdHash)

  //--------------------GraphEditor--------------------

  /**
   * Sends a signal to the vertex with vertex.id=edgeId.targetId
   */
  def sendSignal(signal: Any, edgeId: EdgeId, blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.sendSignal(signal, edgeId)
    } else {
      // Manually send a fire & forget request.
      sendToWorkerForVertexId(SignalMessage(signal, edgeId), edgeId.targetId)
    }
  }

  def addVertex(vertex: Vertex[_, _], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.addVertex(vertex)
    } else {
      // Manually send a fire & forget request.
      val v = write(vertex) // thread that uses an object should instantiate it (performance)
      val request = Request[Worker]((_.addVertex(v)), returnResult = false)
      sendToWorkerForVertexId(request, vertex.id)
    }
  }

  def addEdge(sourceId: Any, edge: Edge[_], blocking: Boolean = false) {
    // thread that uses an object should instantiate it (performance)
    if (blocking) {
      // use proxy
      workerApi.addEdge(sourceId, edge)
    } else {
      // manually send a fire & forget request -- not delegated so we don't have to serialize twice
      val e = write(edge)
      val request = Request[Worker]((_.addEdge(sourceId, e)), returnResult = false)
      sendToWorkerForVertexId(request, sourceId)
    }
  }

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_], blocking: Boolean = false) {
    if (blocking) {
      // use proxy
      workerApi.addPatternEdge(sourceVertexPredicate, edgeFactory)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker](_.addPatternEdge(sourceVertexPredicate, edgeFactory), returnResult = false)
      sendToWorkers(request)
    }
  }

  def removeVertex(vertexId: Any, blocking: Boolean = false) {
    if (blocking) {
      // use proxy
      workerApi.removeVertex(vertexId)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker]((_.removeVertex(vertexId)), returnResult = false)
      sendToWorkerForVertexId(request, vertexId)
    }
  }

  def removeEdge(edgeId: EdgeId, blocking: Boolean = false) {
    if (blocking) {
      // use proxy
      workerApi.removeEdge(edgeId)
    } else {
      // manually send a fire & forget request     
      val request = Request[Worker]((_.removeEdge(edgeId)), returnResult = false)
      sendToWorkerForVertexId(request, edgeId.sourceId)
    }
  }
  
  def removeVertices(shouldRemove: Vertex[_, _] => Boolean, blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.removeVertices(shouldRemove)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker](_.removeVertices(shouldRemove), returnResult = false)
      sendToWorkers(request)
    }
  }

  def loadGraph(vertexIdHint: Option[Any] = None, graphLoader: GraphEditor => Unit, blocking: Boolean = false) {
    if (blocking) {
      workerApi.loadGraph(vertexIdHint, graphLoader)
    } else {
      val request = Request[Worker]((_.loadGraph(graphLoader)), returnResult = false)
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

  def getGraphEditor: GraphEditor = this

  def getWorkerApi: WorkerApi = workerApi

  def getWorkerProxies: Array[Worker] = workerProxies
}