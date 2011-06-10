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

package signalcollect.implementations.graph

import signalcollect.interfaces._
import signalcollect.implementations.storage.DefaultSerializer

trait DefaultGraphApi extends GraphApi with DefaultSerializer {
  protected def messageBus: MessageBus[Any, Any]

  /**
   * Sends a signal to the vertex with vertex.id=targetId.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) {
    messageBus.sendToWorkerForId(Signal(sourceId, targetId, signal), targetId)
  }

  /**
   * Sends a signal to all vertices.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL) {
    messageBus.sendToWorkers(Signal(sourceId, ALL, signal))
  }

  def add(vertex: Vertex[_, _]) {
    val operation = CommandAddVertex(write(vertex))
    messageBus.sendToWorkerForId(operation, vertex.id)
  }

  def add(edge: Edge[_, _]) {
    val operation = CommandAddEdge(write(edge))
    messageBus.sendToWorkerForId(operation, edge.sourceId)
  }

  def addPatternEdge[IdType, SourceVertexType <: Vertex[IdType, _]](sourceVertexPredicate: Vertex[IdType, _] => Boolean, edgeFactory: IdType => Edge[IdType, _]) {
    messageBus.sendToWorkers(CommandAddPatternEdge(sourceVertexPredicate, edgeFactory))
  }

  def removeVertex(vertexId: Any) {
    messageBus.sendToWorkerForId(CommandRemoveVertex(vertexId), vertexId)
  }

  def removeEdge(edgeId: (Any, Any, String)) {
    messageBus.sendToWorkerForId(CommandRemoveOutgoingEdge(edgeId: (Any, Any, String)), edgeId._1)
  }

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) {
    messageBus.sendToWorkers(CommandRemoveVertices(shouldRemove))
  }

}