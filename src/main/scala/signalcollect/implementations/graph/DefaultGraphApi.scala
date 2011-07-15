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
import signalcollect.implementations.serialization.DefaultSerializer

object DefaultGraphApi {
  protected class InstantiatableGraphApi(val messageBus: MessageBus[Any]) extends DefaultGraphApi
  def createInstance(messageBus: MessageBus[Any]): GraphApi = new InstantiatableGraphApi(messageBus)
}

trait DefaultGraphApi extends GraphApi with DefaultSerializer {
  protected def messageBus: MessageBus[Any]

  /**
   * Sends a signal to the vertex with vertex.id=targetId.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) {
    messageBus.sendToWorkerForVertexId(Signal(sourceId, targetId, signal), targetId)
  }

  /**
   * Sends a signal to all vertices.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL) {
    messageBus.sendToWorkers(Signal(sourceId, ALL, signal))
  }

  def addVertex(vertex: Vertex[_, _]) {
    val request = WorkerRequest((_.addVertex(write(vertex))))
    messageBus.sendToWorkerForVertexId(request, vertex.id)
  }

  def addEdge(edge: Edge[_, _]) {
    val request = WorkerRequest((_.addEdge(write(edge))))
    messageBus.sendToWorkerForVertexId(request, edge.sourceId)
  }

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_, _]) {
    val request = WorkerRequest(_.addPatternEdge(sourceVertexPredicate, edgeFactory))
    messageBus.sendToWorkers(request)
  }

  def removeVertex(vertexId: Any) {
    val request = WorkerRequest((_.removeVertex(vertexId)))
    messageBus.sendToWorkerForVertexId(request, vertexId)
  }

  def removeEdge(edgeId: (Any, Any, String)) {
    val request = WorkerRequest((_.removeOutgoingEdge(edgeId: (Any, Any, String))))
    messageBus.sendToWorkerForVertexId(request, edgeId._1)
  }

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) {
    val request = WorkerRequest(_.removeVertices(shouldRemove))
    messageBus.sendToWorkers(request)
  }

}