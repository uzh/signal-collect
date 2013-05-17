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

import com.signalcollect.interfaces.EdgeId

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
trait GraphEditor[@specialized(Int, Long) Id, @specialized(Int, Long, Float, Double) Signal] {

  /**
   *  Sends `signal` to the vertex with `vertex.id==edgeId.targetId`.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean)

  /**
   *  Sends `signal` to the vertex with `vertex.id==edgeId.targetId`.
   *
   *  @note Does not block.
   */
  def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id]) {
    sendSignal(signal, targetId, sourceId, false)
  }

  /**
   *  Adds `vertex` to the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex[Id, _], blocking: Boolean)

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note Does not block.
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex[Id, _]) {
    addVertex(vertex, false)
  }

  /**
   *  Adds `edge` to the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(sourceVertexId: Id, edge: Edge[Id], blocking: Boolean)

  /**
   *  Adds `edge` to the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(sourceVertexId: Id, edge: Edge[Id]) {
    addEdge(sourceVertexId, edge, false)
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Id, blocking: Boolean)

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Id) {
    removeVertex(vertexId, false)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Id], blocking: Boolean)

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note Does not block.
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Id]) {
    removeEdge(edgeId, false)
  }

  /**
   *  Loads a graph using the provided `graphModification` function.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id], blocking: Boolean)

  /**
   *  Loads a graph using the provided `graphModification` function.
   *
   *  @note Does not block.
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id]) {
    modifyGraph(graphModification, vertexIdHint, false)
  }

  /**
   *  Loads a graph using the provided iterator of `graphModification` functions.
   *
   *  @note IMPORTANT: Only works while the computation is not yet executing.
   *  @note IMPORTANT: Need to call `awaitIdle` after all load commands are submitted and before executing.
   *  @note Does not block.
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def loadGraph(graphModifications: Iterator[GraphEditor[Id, Signal] => Unit], vertexIdHint: Option[Id])

  /**
   * Forces the underlying MessageBus to send all messages immediately.
   */
  private[signalcollect] def flush

  private[signalcollect] def sendToWorkerForVertexIdHash(m: Any, vertexIdHash: Int)

}