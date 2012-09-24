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
 *  Uses delegation instead of default parameters to be Java compatible.
 *
 *  @see Graph
 *
 *  @example `graphEditor.removeVertex(1)`
 *
 *  @author Philip Stutz
 */
trait GraphEditor {

  /**
   *  Sends `signal` along the edge with id `edgeId`.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any], blocking: Boolean)

  /**
   *  Sends `signal` along the edge with id `edgeId`.
   *
   *  @note Does not block.
   */
  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any]) {
    sendSignalAlongEdge(signal, edgeId, false)
  }

  /**
   *  Sends `signal` to the vertex with `vertex.id=targetId` along a virtual edge with source id `"External"`.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def sendSignalToVertex(signal: Any, targetId: Any, blocking: Boolean) {
    sendSignalAlongEdge(signal, new DefaultEdgeId("External", targetId))
  }

  /**
   *  Sends `signal` to the vertex with `vertex.id=targetId` along a virtual edge with source id `"External"`.
   *
   *  @note Does not block.
   */
  def sendSignalToVertex(signal: Any, targetId: Any) {
    sendSignalToVertex(signal, targetId, false)
  }

  /**
   *  Adds `vertex` to the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex, blocking: Boolean)

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note Does not block.
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex) {
    addVertex(vertex, false)
  }

  /**
   *  Adds `edge` to the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(edge: Edge, blocking: Boolean)

  /**
   *  Adds `edge` to the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(edge: Edge) {
    addEdge(edge, false)
  }

  private[signalcollect] def addIncomingEdge(edge: Edge, blocking: Boolean = false)

  /**
   *  Adds edges to vertices that satisfy `sourceVertexPredicate`. The edges added are created by `edgeFactory`,
   *  which will receive the respective vertex as a parameter.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge, blocking: Boolean)

  /**
   *  Adds edges to vertices that satisfy `sourceVertexPredicate`. The edges added are created by `edgeFactory`,
   *  which will receive the respective vertex as a parameter.
   *
   *  @note Does not block.
   */
  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge) {
    addPatternEdge(sourceVertexPredicate, edgeFactory, false)
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Any, blocking: Boolean)

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Any) {
    removeVertex(vertexId, false)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Any, Any], blocking: Boolean)

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Any, Any]) {
    removeEdge(edgeId, false)
  }

  private[signalcollect] def removeIncomingEdge(edgeId: EdgeId[Any, Any], blocking: Boolean = false)

  /**
   *  Removes all vertices that satisfy the `shouldRemove` predicate from the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def removeVertices(shouldRemove: Vertex => Boolean, blocking: Boolean)

  /**
   *  Removes all vertices that satisfy the `shouldRemove` predicate from the graph.
   *
   *  @note Does not block.
   */
  def removeVertices(shouldRemove: Vertex => Boolean) {
    removeVertices(shouldRemove, false)
  }

  /**
   *  Loads a graph using the provided graphLoader function.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note The vertexIDHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def loadGraph(vertexIdHint: Option[Any] = None, graphLoader: GraphEditor => Unit, blocking: Boolean)

  /**
   *  Loads a graph using the provided graphLoader function.
   *
   *  @note Does not block.
   *  @note The vertexIDHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def loadGraph(vertexIdHint: Option[Any], graphLoader: GraphEditor => Unit) {
    loadGraph(vertexIdHint, graphLoader, false)
  }

}