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

package com.signalcollect.implementations.messaging

import com.signalcollect.interfaces._
import java.util.HashMap
import com.signalcollect.implementations.logging.DefaultLogger
import com.signalcollect.interfaces.LogMessage
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorRef
import com.signalcollect.implementations.coordinator.WorkerApi
import com.signalcollect.GraphEditor
import com.signalcollect.EdgeId
import com.signalcollect.Vertex
import com.signalcollect.Edge
import com.signalcollect.implementations.serialization.DefaultSerializer
import java.util.Random

class DefaultMessageBus(
  val numberOfWorkers: Int)
  extends MessageBus with GraphEditor with DefaultSerializer {

  protected var registrations = new AtomicInteger()

  def isInitialized = registrations.get == numberOfWorkers + 2

  protected val mapper: VertexToWorkerMapper = new DefaultVertexToWorkerMapper(numberOfWorkers)

  protected val workers = new Array[ActorRef](numberOfWorkers)

  lazy val parallelWorkers = workers.par

  lazy val workerProxies = workers map (AkkaProxy.newInstance[Worker](_, sentMessagesCounter, receivedMessagesCounter))

  lazy val workerApi = new WorkerApi(workerProxies, mapper)

  protected var logger: ActorRef = _

  protected var coordinator: ActorRef = _

  protected val sentMessagesCounter = new AtomicInteger(0)
  def getSentMessagesCounter: AtomicInteger = sentMessagesCounter
  protected val receivedMessagesCounter = new AtomicInteger(0)
  def getReceivedMessagesCounter: AtomicInteger = receivedMessagesCounter

  def messagesSent = sentMessagesCounter.get
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

  def sendToCoordinator(message: Any) {
    sentMessagesCounter.incrementAndGet
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
    val worker = workers(mapper.getWorkerIdForVertexId(recipientId))
    sentMessagesCounter.incrementAndGet
    worker ! message
  }

  def sendToWorkerForVertexIdHash(message: Any, recipientIdHash: Int) {
    val worker = workers(mapper.getWorkerIdForVertexIdHash(recipientIdHash))
    sentMessagesCounter.incrementAndGet
    worker ! message
  }

  def sendToWorker(workerId: Int, message: Any) {
    sentMessagesCounter.incrementAndGet
    workers(workerId) ! message
  }

  def sendToWorkers(message: Any) {
    sentMessagesCounter.addAndGet(numberOfWorkers)
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
  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any], blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.sendSignalAlongEdge(signal, edgeId)
    } else {
      // manually send a fire & forget request
      sendToWorkerForVertexId(SignalMessage(edgeId, signal), edgeId.targetId)
    }
  }

  def addVertex(vertex: Vertex, blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.addVertex(vertex)
    } else {
      // manually send a fire & forget request
      val v = write(vertex) // thread that uses an object should instantiate it (performance)
      val request = Request[Worker]((_.addVertex(v)), returnResult = false)
      sendToWorkerForVertexId(request, vertex.id)
    }
  }

  def addEdge(edge: Edge, blocking: Boolean = false) {
    // thread that uses an object should instantiate it (performance)
    if (blocking == true) {
      // use proxy
      workerApi.addEdge(edge)
    } else {
      // manually send a fire & forget request
      val e = write(edge)
      val request = Request[Worker]((_.addOutgoingEdge(e)), returnResult = false)
      sendToWorkerForVertexId(request, edge.id.sourceId)
    }
  }

  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge, blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.addPatternEdge(sourceVertexPredicate, edgeFactory)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker](_.addPatternEdge(sourceVertexPredicate, edgeFactory), returnResult = false)
      sendToWorkers(request)
    }
  }

  def removeVertex(vertexId: Any, blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.removeVertex(vertexId)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker]((_.removeVertex(vertexId)), returnResult = false)
      sendToWorkerForVertexId(request, vertexId)
    }
  }

  def removeEdge(edgeId: EdgeId[Any, Any], blocking: Boolean = false) {
    if (blocking == true) {
      // use proxy
      workerApi.removeEdge(edgeId)
    } else {
      // manually send a fire & forget request
      val request = Request[Worker]((_.removeOutgoingEdge(edgeId)), returnResult = false)
      sendToWorkerForVertexId(request, edgeId.sourceId)
    }
  }

  def removeVertices(shouldRemove: Vertex => Boolean, blocking: Boolean = false) {
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
    if (blocking == true) {
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