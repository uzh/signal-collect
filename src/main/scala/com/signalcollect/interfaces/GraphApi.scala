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

package com.signalcollect.interfaces

trait GraphApi {

  /**
   * Sends a signal to the vertex with vertex.id=targetId.
   * The senderId of this signal will be com.signalcollect.interfaces.External
   */
  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL)

  /**
   * Sends a signal to all vertices.
   * The senderId of this signal will be com.signalcollect.interfaces.External
   */
  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL)

  def addVertex(vertex: Vertex[_, _])

  def addEdge(edge: Edge[_, _])

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_, _])

  def removeVertex(vertexId: Any)

  def removeEdge(edgeId: (Any, Any, String))

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean)

}