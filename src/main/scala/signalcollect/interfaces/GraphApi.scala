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

package signalcollect.interfaces

trait GraphApi {

  /**
   * Adds a vertex with type VertexType.
   * The constructor is called with the parameter sequence id::otherConstructorParameters.
   *
   * If a vertex with the same id already exists, then this operation is ignored.
   */
  def addVertex[VertexType <: Vertex[_, _]](vertexId: Any, otherConstructorParameters: Any*)(implicit m: Manifest[VertexType])

  /**
   * Adds an edge with type EdgeType.
   *
   * The constructor is called with the parameter sequence sourceVertexId::targetVertexId::otherConstructorParameters.
   */
  def addEdge[EdgeType <: Edge[_, _]](sourceVertexId: Any, targetVertexId: Any, otherConstructorParameters: Any*)(implicit m: Manifest[EdgeType])

  def addVertex[VertexClass <: Class[Vertex[_, _]]](vertexClass: VertexClass, vertexId: Any, otherConstructorParameters: Any*)

  def addEdge[EdgeClass <: Class[Edge[_, _]]](edgeClass: EdgeClass, sourceVertexId: Any, targetVertexId: Any, otherConstructorParameters: Any*)

  
  def addPatternEdge[IdType, SourceVertexType <: Vertex[IdType, _]](sourceVertexPredicate: Vertex[IdType, _] => Boolean, edgeFactory: IdType => Edge[IdType, _])

  def removeVertex(vertexId: Any)

  def removeEdge(edgeId: (Any, Any, String))

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean)

}