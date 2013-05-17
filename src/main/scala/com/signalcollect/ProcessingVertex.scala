/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

import scala.collection.mutable.ArrayBuffer

/**
 *  Simple data-flow vertex implementation that merely processes messages.
 *  Edge additions and edge representations are not implemented.
 *
 *  Delivered signals get accumulated inside the state of this vertex. When the vertex signals,
 *  the `process` function is called for each item in LIFO order (List has better performance
 *  than ArrayBuffer for this use case).
 *
 *  @param id Unique vertex id.
 *
 *  @author Philip Stutz
 */
abstract class ProcessingVertex[Id, SignalType](
  val id: Id,
  var state: List[SignalType] = List[SignalType]())
  extends Vertex[Id, List[SignalType]] {

  def process(signal: SignalType, graphEditor: GraphEditor[Any, Any])

  def shouldProcess(signal: SignalType): Boolean = true

  def setState(s: List[SignalType]) {
    state = s
  }

  def deliverSignal(signal: Any, sourceId: Option[Any], graphEditor: GraphEditor[Any, Any]): Boolean = {
    val item = signal.asInstanceOf[SignalType]
    if (shouldProcess(item)) {
      state = item :: state
    }
    true
  }

  override def executeSignalOperation(graphEditor: GraphEditor[Any, Any]) {
    for (item <- state) {
      process(item, graphEditor)
    }
    state = List.empty
  }

  override def scoreSignal: Double = if (state.isEmpty) 0 else 1
  def scoreCollect = 0 // Because signals are collected upon delivery.
  def edgeCount = 0
  override def toString = s"${this.getClass.getName}(state=$state)"
  def executeCollectOperation(graphEditor: GraphEditor[Any, Any]) {}
  def beforeRemoval(graphEditor: GraphEditor[Any, Any]) = {}
  override def afterInitialization(graphEditor: GraphEditor[Any, Any]) {}
  override def addEdge(e: Edge[_], graphEditor: GraphEditor[Any, Any]): Boolean = throw new UnsupportedOperationException
  override def removeEdge(targetId: Any, graphEditor: GraphEditor[Any, Any]): Boolean = throw new UnsupportedOperationException
  override def removeAllEdges(graphEditor: GraphEditor[Any, Any]): Int = 0
}
