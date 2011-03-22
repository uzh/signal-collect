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

import signalcollect.implementations.coordinator.GenericConstructor
import signalcollect._
import signalcollect.interfaces._

trait DefaultGraphApi extends GraphApi {
  protected def messageBus: MessageBus[Any, Any]
  
  def addVertex[VertexType <: Vertex[_, _]](vertexId: Any, otherConstructorParameters: Any*)(implicit m: Manifest[VertexType]) {
    val parameters: List[AnyRef] = vertexId.asInstanceOf[AnyRef] :: otherConstructorParameters.toList.asInstanceOf[List[AnyRef]]
    val constructor = GenericConstructor.newInstance[VertexType] _
    val operation = CommandAddVertexFromFactory(constructor, parameters)
    messageBus.sendToWorkerForId(operation, vertexId)
  }

  def addEdge[EdgeType <: Edge[_, _]](sourceVertexId: Any, targetVertexId: Any, otherConstructorParameters: Any*)(implicit m: Manifest[EdgeType]) {
    val parameters: List[AnyRef] = sourceVertexId.asInstanceOf[AnyRef] :: targetVertexId.asInstanceOf[AnyRef] :: otherConstructorParameters.toList.asInstanceOf[List[AnyRef]]
    val constructor = GenericConstructor.newInstance[EdgeType] _
    val operation = CommandAddEdgeFromFactory(constructor, parameters)
    messageBus.sendToWorkerForId(operation, sourceVertexId)
  }

  override def addPatternEdge[IdType, SourceVertexType <: Vertex[IdType, _]](sourceVertexPredicate: Vertex[IdType, _] => Boolean, edgeFactory: IdType => Edge[IdType, _]) {
    messageBus.sendToWorkers(CommandAddPatternEdge(sourceVertexPredicate, edgeFactory))
  }

  override def removeVertex(vertexId: Any) {
    messageBus.sendToWorkerForId(CommandRemoveVertex(vertexId), vertexId)
  }

  override def removeEdge(edgeId: (Any, Any, String)) {
    messageBus.sendToWorkerForId(CommandRemoveOutgoingEdge(edgeId: (Any, Any, String)), edgeId._1)
  }

  override def removeVertices[VertexType <: Vertex[_, _]](predicate: VertexType => Boolean) {
    messageBus.sendToWorkers(CommandRemoveVertices(predicate))
  }

  override def removeEdges[EdgeType <: Edge[_, _]](predicate: EdgeType => Boolean) {
    messageBus.sendToWorkers(CommandRemoveOutgoingEdges(predicate))
  }

}