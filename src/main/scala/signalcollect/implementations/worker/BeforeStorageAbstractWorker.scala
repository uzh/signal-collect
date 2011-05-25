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

import signalcollect.util.constructors.ConstructorFinder
import signalcollect.implementations.messaging.AbstractMessageRecipient
import java.util.concurrent.TimeUnit
import signalcollect.implementations._
import signalcollect.interfaces._
import signalcollect.api.Factory._
import java.util.concurrent.BlockingQueue
import java.util.HashSet
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set
import signalcollect.interfaces.ALL
import signalcollect.implementations.graph.DefaultGraphApi

abstract class BeforeStorageAbstractWorker(
  protected val messageBus: MessageBus[Any, Any],
  messageInboxFactory: QueueFactory,
  storageFactory: StorageFactory)
  extends AbstractMessageRecipient(messageInboxFactory)
  with Worker
  with Logging
  with DefaultGraphApi
  with Traversable[Vertex[_, _]] {

  override protected def process(message: Any) {
    message match {
      case s: Signal[_, _, _] => processSignal(s)
      case CommandShutDown => shutDown = true
      case CommandStartComputation => startComputation
      case CommandPauseComputation => pauseComputation
      case CommandForEachVertex(f) => foreach(f)
      case CommandAddVertex(vertex) => addLocalVertex(vertex)
      case CommandAddEdge(edge) => addOutgoingEdge(edge)
      case CommandAddVertexFromFactory(vertexClass, parameters) => addLocalVertex(vertexClass, parameters)
      case CommandAddEdgeFromFactory(edgeClass, parameters) => addOutgoingEdge(edgeClass, parameters)
      case CommandAddPatternEdge(sourceVertexPredicate, vertexFactory) => addOutgoingEdges(sourceVertexPredicate, vertexFactory)
      case CommandRemoveVertex(vertexId) => removeLocalVertex(vertexId)
      case CommandRemoveOutgoingEdge(edgeId) => removeOutgoingEdge(edgeId)
      case CommandRemoveVertices(shouldRemove) => removeLocalVertices(shouldRemove)
      case CommandAddIncomingEdge(edgeId) => addIncomingEdge(edgeId)
      case CommandSetUndeliverableSignalHandler(h) => undeliverableSignalHandler = h
      case CommandSetSignalThreshold(sT) => signalThreshold = sT
      case CommandSetCollectThreshold(cT) => collectThreshold = cT
      case CommandSignalStep => executeSignalStep
      case CommandCollectStep => executeCollectStep
      case CommandSendComputationProgressStats => sendComputationProgressStats
      case CommandAggregate(neutralElement, aggregator, extractor) => aggregate(neutralElement, aggregator, extractor)
      case other => log("Could not handle message " + message)
    }
  }

  protected var undeliverableSignalHandler: (Signal[_, _, _], GraphApi) => Unit = (s, g) => {}

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

  protected def isConverged = toCollect.isEmpty && toSignal.isEmpty

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

//  protected val idleTimeoutNanoseconds: Long = 1000l * 1000l * 100l //100ms // * 50l //1000000 * 50000 // 50 milliseconds
  val idleTimeoutNanoseconds: Long = 1000000l * 10l// * 50l //1000000 * 50000 // 50 milliseconds


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
      val i = vertices.iterator
      while (i.hasNext) {
        val vertex = i.next
        f(vertex)
      }
  }

  protected def removeLocalVertices(shouldRemove: Vertex[_, _] => Boolean) {
    foreach { vertex =>
      if (shouldRemove(vertex)) {
        processRemoveVertex(vertex)
      }
    }
  }

  protected var verticesRemovedCounter = 0l

  protected def removeLocalVertex(vertexId: Any) {
    val vertex = vertexMap.get(vertexId)
    if (vertex != null) {
      processRemoveVertex(vertex)
    } else {
      log("Should remove vertex with id " + vertexId + ": could not find this vertex.")
    }
  }

  protected def processRemoveVertex(vertex: Vertex[_, _]) {
    outgoingEdgesRemovedCounter += vertex.outgoingEdgeCount
    vertex.removeAllOutgoingEdges
    verticesRemovedCounter += 1
    vertices.remove(vertex.id)
  }

  protected var outgoingEdgesRemovedCounter = 0l

  protected def removeOutgoingEdge(edgeId: (Any, Any, String)) {
    var removed = false
    val vertex = vertexMap.get(edgeId._1)
    if (vertex != null) {
      if (vertex.removeOutgoingEdge(edgeId)) {
        outgoingEdgesRemovedCounter += 1
      } else {
        log("Outgoing edge not found when trying to remove edge with id " + edgeId)
      }
    } else {
      log("Source vertex not found found when trying to remove edge with id " + edgeId)
    }
  }

  protected def removeIncomingEdge(edgeId: (Any, Any, String)) {
    val targetVertexId = edgeId._2
    val targetVertex = vertexMap.get(targetVertexId)
    if (targetVertex != null) {
      val removed = targetVertex.removeIncomingEdge(edgeId)
    } else {
      log("Did not find vertex with id " + targetVertexId + " when modifying number of incoming edges")
    }
  }

  var incomingEdgesAddedCounter = 0l

  protected def addIncomingEdge(edgeId: (Any, Any, String)) {
    val targetVertexId = edgeId._2
    val targetVertex = vertexMap.get(targetVertexId)
    if (targetVertex != null) {
      incomingEdgesAddedCounter += 1
      targetVertex.addIncomingEdge(edgeId)
    } else {
      log("Did not find vertex with id " + targetVertexId + " when modifying number of incoming edges")
    }
  }

  protected def addOutgoingEdge(edgeClass: Class[_ <: Edge[_, _]], parameters: Seq[AnyRef]) {
    val edge = ConstructorFinder.newInstanceFromClass(edgeClass)(parameters)
    addOutgoingEdge(edge)
  }

  var outgoingEdgesAddedCounter = 0l

  protected def addOutgoingEdge(e: Edge[_, _]) = {
    val key = e.sourceId
    val vertex = vertexMap.get(key)
    if (vertex != null) {
      outgoingEdgesAddedCounter += 1
      vertex.addOutgoingEdge(e)
      messageBus.sendToWorkerForIdHash(CommandAddIncomingEdge(e.id), e.targetHashCode)
      toCollect.add(vertex)
      toSignal.add(vertex)
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

  protected var toCollect = vertexSetFactory
  protected var toSignal = vertexSetFactory
  protected def vertexSetFactory: Set[Vertex[_, _]] = new HashSet[Vertex[_, _]]()
  val vertexMap = new HashMap[Any, Vertex[_, _]]()
  protected def vertices = vertexMap.values

  protected def executeSignalStep {
    var converged = toSignal.isEmpty
    val i = toSignal.iterator
    while (i.hasNext) {
      val vertex = i.next
      signal(vertex)
    }
    toSignal.clear
    messageBus.sendToCoordinator(StatusSignalStepDone)
  }

  protected def executeCollectStep {
    val i = toCollect.iterator
    while (i.hasNext) {
      val vertex = i.next
      collect(vertex)
      toSignal.add(vertex)
    }
    toCollect.clear
    messageBus.sendToCoordinator(StatusCollectStepDone(toSignal.size))
  }

  protected def addLocalVertex(vertexClass: Class[_ <: Vertex[_, _]], parameters: Seq[AnyRef]) {
    val vertex = ConstructorFinder.newInstanceFromClass(vertexClass)(parameters)
    addLocalVertex(vertex)
  }

  var verticesAddedCounter = 0l

  protected def addLocalVertex(vertex: Vertex[_, _]) {
    if (!vertexMap.containsKey(vertex.id)) {
      verticesAddedCounter += 1
      vertexMap.put(vertex.id, vertex)
      vertex.setMessageBus(messageBus)
      toCollect.add(vertex)
      toSignal.add(vertex)
      vertex.afterInitialization
    }
  }

  var collectOperationsExecutedCounter = 0l

  protected def collect(vertex: Vertex[_, _]): Boolean = {
    if (vertex.scoreCollect > collectThreshold) {
      collectOperationsExecutedCounter += 1
      vertex.executeCollectOperation
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
      true
    } else {
      false
    }
  }

  protected def sendStatsToCoordinator {
    messageBus.sendToCoordinator(StatusNumberOfVertices(vertices.size))
    messageBus.sendToCoordinator(StatusNumberOfEdges(countOutgoingEdges))
  }

  def sendComputationProgressStats {
    val stats = ComputationProgressStats(
      toCollect.size,
      collectOperationsExecutedCounter,
      toSignal.size,
      signalOperationsExecutedCounter,
      verticesAddedCounter,
      verticesRemovedCounter,
      outgoingEdgesAddedCounter,
      outgoingEdgesRemovedCounter,
      incomingEdgesAddedCounter)
    messageBus.sendToCoordinator(stats)
  }

  protected def countOutgoingEdges = {
    var numberOfEdges = 0
    foreach(vertex => numberOfEdges += vertex.outgoingEdgeCount)
    numberOfEdges
  }

  protected def processSignal(signal: Signal[_, _, _]) {
    if (signal.targetId == ALL) {
      val i = vertices.iterator
      while (i.hasNext) {
        val vertex = i.next
        deliverSignal(signal, vertex)
      }
    } else {
      val vertex = vertexMap.get(signal.targetId)
      if (vertex != null) {
        deliverSignal(signal, vertex)
      } else {
        undeliverableSignalHandler(signal, this)
      }
    }
  }

  protected def deliverSignal(signal: Signal[_, _, _], vertex: Vertex[_, _]) {
    vertex.receive(signal)
    toCollect.add(vertex)
  }

}