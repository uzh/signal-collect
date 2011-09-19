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

package com.signalcollect

import com.signalcollect._

/**
 *  GraphEditor offers functions to modify the graph and to send signals.
 *
 *  @see Graph
 *
 *  @example `graphEditor.removeVertex(1)`
 *
 *  @author Philip Stutz
 *  @version 1.0
 *  @since 1.0
 */
trait GraphEditor {

  /**
   *  Sends `signal` along the edge with id `edgeId`.
   */
  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any])

  /**
   *  Sends `signal` to the vertex with `vertex.id=targetId` along a virtual edge with source id `"Coordinator"`.
   */
  def sendSignalToVertex(signal: Any, targetId: Any) {
    sendSignalAlongEdge(signal, new DefaultEdgeId("Coordinator", targetId))
  }
  
  /**
   *  Adds `vertex` to the graph.
   * 
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex)

  /**
   *  Adds `edge` to the graph.
   * 
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged. 
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged. 
   */
  def addEdge(edge: Edge)

  /**
   *  Adds edges to vertices that satisfy `sourceVertexPredicate`. The edges added are created by `edgeFactory`,
   *  which will receive the respective vertex as a parameter.
   */
  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge)

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *  
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Any)

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *  
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged. 
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged. 
   */
  def removeEdge(edgeId: EdgeId[Any, Any])

  /**
   *  Removes all vertices that satisfy the `shouldRemove` predicate from the graph.
   */
  def removeVertices(shouldRemove: Vertex => Boolean)

}