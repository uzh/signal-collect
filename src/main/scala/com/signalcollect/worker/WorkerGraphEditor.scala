/*
 *  @author Philip Stutz
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

import com.signalcollect._
import com.signalcollect.interfaces._
import akka.actor.ActorRef

/**
 * Wraps a general graph editor and optimizes operations that happen locally to a worker
 * by calling them directly on the worker itself.
 */
final class WorkerGraphEditor[@specialized(Int, Long) Id, Signal](
  workerId: Int,
  worker: WorkerApi[Id, Signal],
  messageBus: MessageBus[Id, Signal])
  extends GraphEditor[Id, Signal] {

  private[signalcollect] val graphEditor = messageBus.getGraphEditor

  val log = graphEditor.log

  @inline override def sendSignal(signal: Signal, targetId: Id, sourceId: Id) {
    graphEditor.sendSignal(signal, targetId, sourceId)
  }

  @inline override def sendSignal(signal: Signal, targetId: Id) {
    graphEditor.sendSignal(signal, targetId)
  }

  @inline override def addVertex(vertex: Vertex[Id, _, Id, Signal]) {
    graphEditor.addVertex(vertex)
  }

  @inline override def addEdge(sourceVertexId: Id, edge: Edge[Id]) {
    graphEditor.addEdge(sourceVertexId, edge)
  }

  @inline override def removeVertex(vertexId: Id) {
    graphEditor.removeVertex(vertexId)
  }

  @inline override def removeEdge(edgeId: EdgeId[Id]) {
    graphEditor.removeEdge(edgeId)
  }

  @inline override def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id]) {
    graphEditor.modifyGraph(graphModification, vertexIdHint)
  }

  @inline override def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    graphEditor.sendSignal(signal, targetId, sourceId, blocking)
  }

  @inline override def addVertex(vertex: Vertex[Id, _, Id, Signal], blocking: Boolean) {
    if (!blocking) {
      graphEditor.addVertex(vertex)
    } else if (shouldHandleLocally(vertex.id)) {
      worker.addVertex(vertex)
    } else {
      graphEditor.addVertex(vertex, blocking)
    }
  }

  @inline override def addEdge(sourceVertexId: Id, edge: Edge[Id], blocking: Boolean) {
    if (!blocking) {
      graphEditor.addEdge(sourceVertexId, edge)
    } else if (shouldHandleLocally(sourceVertexId)) {
      worker.addEdge(sourceVertexId, edge)
    } else {
      graphEditor.addEdge(sourceVertexId, edge, blocking)
    }
  }

  @inline override def removeVertex(vertexId: Id, blocking: Boolean) {
    if (!blocking) {
      graphEditor.removeVertex(vertexId)
    } else if (shouldHandleLocally(vertexId)) {
      worker.removeVertex(vertexId)
    } else {
      graphEditor.removeVertex(vertexId, blocking)
    }
  }

  @inline override def removeEdge(edgeId: EdgeId[Id], blocking: Boolean) {
    if (!blocking) {
      graphEditor.removeEdge(edgeId)
    } else if (shouldHandleLocally(edgeId.sourceId)) {
      worker.removeEdge(edgeId)
    } else {
      graphEditor.removeEdge(edgeId, blocking)
    }
  }

  @inline override def modifyGraph(graphLoader: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None, blocking: Boolean) {
    if (!blocking) {
      graphEditor.modifyGraph(graphLoader, vertexIdHint)
    } else if (vertexIdHint.isDefined && shouldHandleLocally(vertexIdHint.get)) {
      worker.modifyGraph(graphLoader, vertexIdHint)
    } else {
      graphEditor.modifyGraph(graphLoader, vertexIdHint, blocking)
    }
  }

  @inline override def recalculateScoresForVertexWithId(vertexId: Id) {
    graphEditor.recalculateScoresForVertexWithId(vertexId)
  }

  @inline override def loadGraph(graphModifications: Iterator[GraphEditor[Id, Signal] => Unit], vertexIdHint: Option[Id]) {
    graphEditor.loadGraph(graphModifications, vertexIdHint)
  }

  @inline def shouldHandleLocally(vertexId: Id): Boolean = {
    messageBus.getWorkerIdForVertexId(vertexId) == workerId
  }

  @inline override private[signalcollect] def flush {
    graphEditor.flush
  }

  @inline override private[signalcollect] def sendToWorkerForVertexIdHash(message: Any, vertexIdHash: Int) {
    graphEditor.sendToWorkerForVertexIdHash(message, vertexIdHash)
  }

  @inline override private[signalcollect] def sendToActor(actor: ActorRef, message: Any) {
    graphEditor.sendToActor(actor, message)
  }

}