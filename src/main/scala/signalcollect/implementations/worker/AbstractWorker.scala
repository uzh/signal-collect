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

package signalcollect.implementations.worker

import signalcollect.implementations.messaging.AbstractMessageRecipient
import java.util.concurrent.TimeUnit
import signalcollect.implementations._
import signalcollect.interfaces._
import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Storage._
import java.util.concurrent.BlockingQueue
import java.util.HashSet
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set

abstract class AbstractWorker(
  protected val messageBus: MessageBus[Any, Any],
  messageInboxFactory: QueueFactory,
  storageFactory: StorageFactory)
  extends AbstractMessageRecipient(messageInboxFactory)
  with Worker
  with Logging
  with Traversable[Vertex[_, _]] {

  override protected def process(message: Any) {
    message match {
      case s: Signal[_, _, _] => processSignal(s)
      case CommandShutDown => shutDown = true
      case CommandStartComputation => startComputation
      case CommandPauseComputation => pauseComputation
      case CommandForEachVertex(f) => foreach(f)
      case CommandAddVertexFromFactory(vertexFactory, parameters) => addVertex(vertexFactory(parameters))
      case CommandAddEdgeFromFactory(edgeFactory, parameters) => addOutgoingEdge(edgeFactory(parameters))
      case CommandAddPatternEdge(sourceVertexPredicate, vertexFactory) => addOutgoingEdges(sourceVertexPredicate, vertexFactory)
      case CommandRemoveVertex(vertexId) => removeVertex(vertexId)
      case CommandRemoveOutgoingEdge(edgeId) => removeOutgoingEdge(edgeId)
      case CommandRemoveVertices(shouldRemove) => removeVertices(shouldRemove)
      case CommandAddIncomingEdge(edgeId) => addIncomingEdge(edgeId)
      case CommandSetSignalThreshold(sT) => signalThreshold = sT
      case CommandSetCollectThreshold(cT) => collectThreshold = cT
      case CommandSignalStep => executeSignalStep
      case CommandCollectStep => executeCollectStep
      case CommandSendComputationProgressStats => sendComputationProgressStats
      case CommandAggregate(neutralElement, aggregator, extractor) => aggregate(neutralElement, aggregator, extractor)
      case other => log("Could not handle message " + message)
    }
  }

  protected def aggregate[ValueType](neutralElement: ValueType, aggregator: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType) {
    val aggregatedValue = foldLeft(neutralElement) { (a: ValueType, v: Vertex[_, _]) => aggregator(a, extractor(v)) }
    messageBus.sendToCoordinator(StatusAggregatedValue(aggregatedValue))
  }

  protected def startComputation {
    shouldStart = true
  }

  protected def pauseComputation {
    shouldPause = true
  }

  protected var isIdle = false
  protected var shutDown = false
  protected var isPaused = true
  protected var shouldPause = false
  protected var shouldStart = false

  protected var signalThreshold = 0.001
  protected var collectThreshold = 0.0

  protected var resultProcessingDone = false

  protected var vertexStore = storageFactory(messageBus)

  protected def isConverged = !vertexStore.hasToCollect && !vertexStore.hasToSignal

  protected def setIdle(newIdleState: Boolean) {
    if (isIdle != newIdleState) {
      if (newIdleState == true) {
        messageBus.sendToCoordinator(StatusWorkerIsIdle)
      } else {
        messageBus.sendToCoordinator(StatusWorkerIsBusy)
      }
      isIdle = newIdleState
    }
  }

  protected val idleTimeoutNanoseconds: Long = 1000000l * 10l // * 50l //1000000 * 50000 // 50 milliseconds

  protected def processInboxOrIdle(idleTimeoutNanoseconds: Long) {
    var message = messageInbox.poll(idleTimeoutNanoseconds, TimeUnit.NANOSECONDS)
    if (message == null) {
      setIdle(true)
      handleMessage
      setIdle(false)
    } else {
      process(message)
      processInbox
    }
  }

  def foreach[U](f: (Vertex[_, _]) => U) {
    vertexStore.foreach(f)
  }

  protected def removeVertices(shouldRemove: Vertex[_, _] => Boolean) {
    foreach { vertex =>
      if (shouldRemove(vertex)) {
        processRemoveVertex(vertex)
      }
    }
  }

  protected var verticesRemovedCounter = 0l

  protected def removeVertex(vertexId: Any) {
    val vertex = vertexStore.getVertexWithID(vertexId)
    if (vertex != null) {
      processRemoveVertex(vertex)
    } else {
      log("Should remove vertex with id " + vertexId + ": could not find this vertex.")
    }
  }

  protected def processRemoveVertex(vertex: Vertex[_, _]) {
    vertex.incomingEdgeCount foreach (incomingEdgesRemovedCounter += _)
    vertex.outgoingEdgeCount foreach (outgoingEdgesRemovedCounter += _)
    vertex.removeAllOutgoingEdges
    verticesRemovedCounter += 1
    vertexStore.removeVertexFromStore(vertex.id)
  }

  protected var outgoingEdgesRemovedCounter = 0l

  protected def removeOutgoingEdge(edgeId: (Any, Any, String)) {
    var removed = false
    val vertex = vertexStore.getVertexWithID(edgeId._1)
    if (vertex != null) {
      if (vertex.removeOutgoingEdge(edgeId)) {
        outgoingEdgesRemovedCounter += 1
        vertexStore.updateStateOfVertex(vertex)
      } else {
        log("Outgoing edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      log("Source vertex not found found when trying to remove edge with id " + edgeId)
    }
  }

  protected var incomingEdgesRemovedCounter = 0l

  protected def removeIncomingEdge(edgeId: (Any, Any, String)) {
    val targetVertexId = edgeId._2
    val targetVertex = vertexStore.getVertexWithID(targetVertexId)
    if (targetVertex != null) {
      val removed = targetVertex.removeIncomingEdge(edgeId)
      removed map (if (_) incomingEdgesRemovedCounter += 1)
      vertexStore.updateStateOfVertex(targetVertex)
    } else {
      log("Did not find vertex with id " + targetVertexId + " when modifying number of incoming edges")
    }
  }

  var incomingEdgesAddedCounter = 0l

  protected def addIncomingEdge(edgeId: (Any, Any, String)) {
    val targetVertexId = edgeId._2
    val targetVertex = vertexStore.getVertexWithID(targetVertexId)
    if (targetVertex != null) {
      incomingEdgesAddedCounter += 1
      targetVertex.addIncomingEdge(edgeId)
      vertexStore.updateStateOfVertex(targetVertex)
    } else {
      log("Did not find vertex with id " + targetVertexId + " when modifying number of incoming edges")
    }
  }

  var outgoingEdgesAddedCounter = 0l

  protected def addOutgoingEdge(e: Edge[_, _]) = {
    val key = e.sourceId
    val vertex = vertexStore.getVertexWithID(key)
    if (vertex != null) {
      outgoingEdgesAddedCounter += 1
      vertex.addOutgoingEdge(e)
      messageBus.sendToWorkerForIdHash(CommandAddIncomingEdge(e.id), e.targetHashCode)
      vertexStore.addForCollecting(vertex.id)
      vertexStore.addForSignling(vertex.id)
      vertexStore.updateStateOfVertex(vertex)

    } else {
      log("Did not find vertex with id " + e.sourceId + " when adding edge " + e)
    }
  }

  def addOutgoingEdges[IdType, VertexType <: Vertex[IdType, _]](sourceVertexPredicate: VertexType => Boolean, edgeFactory: IdType => Edge[_, _]) {
    foreach(vertex => {
      try {
        val castVertex = vertex.asInstanceOf[VertexType]
        if (sourceVertexPredicate(castVertex)) {
          addOutgoingEdge(edgeFactory(vertex.id.asInstanceOf[IdType]))
        }
      } catch {
        case badCast =>
      }

    })
  }

  protected def executeSignalStep {
    var converged = !vertexStore.hasToSignal
    vertexStore.foreachToSignal{vertex => signal(vertex) }
    messageBus.sendToCoordinator(StatusSignalStepDone)
  }

  protected def executeCollectStep {
   vertexStore.foreachToCollect(
   vertex => {collect (vertex); 
   vertexStore.addForSignling(vertex.id) }, false)
   messageBus.sendToCoordinator(StatusCollectStepDone(vertexStore.numberOfVerticesToSignal))
  }

  var verticesAddedCounter = 0l

  protected def addVertex(vertex: Vertex[_, _]) {
    if (vertexStore.addVertexToStore(vertex)) {
      verticesAddedCounter += 1
    }
  }

  var collectOperationsExecutedCounter = 0l

  protected def collect(vertex: Vertex[_, _]): Boolean = {
    if (vertex.scoreCollect > collectThreshold) {
      collectOperationsExecutedCounter += 1
      vertex.executeCollectOperation
      vertexStore.updateStateOfVertex(vertex)
      true
    } else {
      false
    }
  }

  var signalOperationsExecutedCounter = 0l

  protected def signal(v: Vertex[_, _]): Boolean = {
    if (v.scoreSignal > signalThreshold) {
      signalOperationsExecutedCounter += 1
      v.executeSignalOperation
      vertexStore.updateStateOfVertex(v)
      true
    } else {
      false
    }
  }

  protected def sendStatsToCoordinator {
    messageBus.sendToCoordinator(StatusNumberOfVertices(vertexStore.getNumberOfVertices))
    messageBus.sendToCoordinator(StatusNumberOfEdges(countOutgoingEdges))
  }

  def sendComputationProgressStats {
    val stats = ComputationProgressStats(
      vertexStore.numberOfVerticesToCollect,
      collectOperationsExecutedCounter,
      vertexStore.numberOfVerticesToSignal,
      signalOperationsExecutedCounter,
      verticesAddedCounter,
      verticesRemovedCounter,
      outgoingEdgesAddedCounter,
      outgoingEdgesRemovedCounter,
      incomingEdgesAddedCounter,
      incomingEdgesRemovedCounter)
    messageBus.sendToCoordinator(stats)
  }

  protected def countOutgoingEdges = {
    var numberOfEdges = 0
    foreach(vertex => numberOfEdges += vertex.outgoingEdgeCount.getOrElse(0))
    numberOfEdges
  }

  protected def processSignal(signal: Signal[_, _, _]) {
    val vertex = vertexStore.getVertexWithID(signal.targetId)
    if (vertex != null) {
      deliverSignal(signal, vertex)
    } else {
      log("Could not deliver signal " + signal + " to vertex with id " + signal.targetId)
    }
    vertexStore.updateStateOfVertex(vertex)
  }

  protected def deliverSignal(signal: Signal[_, _, _], vertex: Vertex[_, _]) {
    vertex.send(signal)
    vertexStore.addForCollecting(vertex.id)
  }

}