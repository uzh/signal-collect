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

package com.signalcollect.implementations.worker

import com.signalcollect._
import com.signalcollect.interfaces._

/**
 * Wraps a general graph editor and optimizes operations that happen locally to a worker
 * by calling them directly on the worker itself.
 */
class WorkerGraphEditor(worker: Worker, messageBus: MessageBus) extends GraphEditor {

  val graphEditor = messageBus.getGraphEditor

  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any], blocking: Boolean = false) = graphEditor.sendSignalAlongEdge(signal, edgeId, blocking)

  def addVertex(vertex: Vertex, blocking: Boolean) = {
    if (blocking && shouldHandleLocally(vertex.id)) {
      worker.addVertex(vertex)
    } else {
      graphEditor.addVertex(vertex, blocking)
    }
  }

  def addEdge(edge: Edge, blocking: Boolean) = {
    if (blocking && shouldHandleLocally(edge.id.sourceId)) {
      worker.addOutgoingEdge(edge)
    } else {
      graphEditor.addEdge(edge, blocking)
    }
  }

  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge, blocking: Boolean = false) = graphEditor.addPatternEdge(sourceVertexPredicate, edgeFactory, blocking)

  def removeVertex(vertexId: Any, blocking: Boolean) {
    if (blocking && shouldHandleLocally(vertexId)) {
      worker.removeVertex(vertexId)
    } else {
      graphEditor.removeVertex(vertexId, blocking)
    }
  }

  def removeEdge(edgeId: EdgeId[Any, Any], blocking: Boolean) {
    if (blocking && shouldHandleLocally(edgeId.sourceId)) {
      worker.removeOutgoingEdge(edgeId)
    } else {
      graphEditor.removeEdge(edgeId, blocking)
    }
  }

  def removeVertices(shouldRemove: Vertex => Boolean, blocking: Boolean) = graphEditor.removeVertices(shouldRemove, blocking)

  def loadGraph(vertexIdHint: Option[Any] = None, graphLoader: GraphEditor => Unit, blocking: Boolean) = graphEditor.loadGraph(vertexIdHint, graphLoader, blocking)

  def shouldHandleLocally(vertexId: Any) = messageBus.getWorkerIdForVertexId(vertexId) == worker.workerId
}