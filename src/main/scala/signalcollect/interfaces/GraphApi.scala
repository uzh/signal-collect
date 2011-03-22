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
	
//  @deprecated("To reduce Cache-to-Cache transfer objects should be allocated by the thread that will read/write them: http://home.engineering.iastate.edu/~morris/papers/10/ieeetc10.pdf\n" +
//  		"Use addVertex[V <: Vertex[_, _]](parameters: Any*) instead.")
//  def addVertex(vertex: Vertex[_, _])
//  
//  @deprecated("To reduce Cache-to-Cache transfer objects should be allocated by the thread that will read/write them: http://home.engineering.iastate.edu/~morris/papers/10/ieeetc10.pdf\n" +
//  		"Use addEdge[E <: Edge[_, _]](parameters: Any*) instead.")
//  def addEdge(edge: Edge[_, _])

//  def addVertex[V <: Vertex[_, _]](clazz: Class[V], parameters: Any*)
//  
//  def addEdge[E <: Edge[_, _]](clazz: Class[E], parameters: Any*)

  def addVertex[VertexType <: Vertex[_, _]](id: Any, otherConstructorParameters: Any*)(implicit m: Manifest[VertexType])
  
  def addEdge[EdgeType <: Edge[_, _]](sourceVertexId: Any, targetVertexId: Any, otherConstructorParameters: Any*)(implicit m: Manifest[EdgeType])
	
//  def addVertex[VertexType <: Vertex[_, _]](clazz: Class[VertexType], vertexId: AnyRef, otherConstructorParameters: AnyRef*)
//  
//  def addEdge[EdgeType <: Edge[_, _]](clazz: Class[EdgeType], sourceVertexId: AnyRef, targetVertexId: AnyRef, otherConstructorParameters: AnyRef*)
  
  
//  def addVertex[V <: Vertex[_, _]](parameters: Any*)(implicit m: Manifest[V])
//  
//  def addEdge[E <: Edge[_, _]](parameters: Any*)(implicit m: Manifest[E])

//  def addVertex(factory: List[AnyRef] => Vertex[_, _], parameters: AnyRef*)
//  
//  def addEdge(factory: List[AnyRef] => Edge[_, _], parameters: AnyRef*)
  
  def addPatternEdge[IdType, SourceVertexType <: Vertex[IdType, _]](sourceVertexPredicate: Vertex[IdType, _] => Boolean, edgeFactory: IdType => Edge[IdType, _])

  def removeVertex(vertexId: Any)
  
  def removeEdge(edgeId: (Any, Any, String))
  
  def removeVertices[VertexType <: Vertex[_, _]](predicate: VertexType => Boolean)
  
  def removeEdges[EdgeType <: Edge[_, _]](predicate: EdgeType => Boolean)
  
}