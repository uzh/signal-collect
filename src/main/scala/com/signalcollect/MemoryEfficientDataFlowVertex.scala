/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

import scala.reflect.ClassTag

import com.signalcollect.util.MemoryEfficientSplayIntSet
import com.signalcollect.util.SplayIntSet

/**
 * A memory efficient implementation of a data flow vertex
 * with an Int id and a memory efficient edge representation
 * that can only represent edges without additional attributes
 * that point to a vertex with an Int id.
 * The signal function is the same for all edges and defined in
 * the 'computeSignal' function.
 */
abstract class MemoryEfficientDataFlowVertex[State, IncomingSignalType: ClassTag](
  val id: Int,
  var state: State) extends Vertex[Int, State, Int, Any] {

  type OutgoingSignalType

  def collect(signal: IncomingSignalType): State

  def computeSignal(targetId: Int): OutgoingSignalType

  var lastSignalState: State = null.asInstanceOf[State]

  def setState(s: State) {
    state = s
  }

  protected var targetIds: SplayIntSet = new MemoryEfficientSplayIntSet

  def deliverSignalWithSourceId(signal: Any, sourceId: Int, graphEditor: GraphEditor[Int, Any]): Boolean = {
    deliverSignalWithoutSourceId(signal, graphEditor)
  }

  def deliverSignalWithoutSourceId(signal: Any, graphEditor: GraphEditor[Int, Any]): Boolean = {
    setState(collect(signal.asInstanceOf[IncomingSignalType]))
    true
  }
  
  /**
   * We always collect on delivery.
   */
  def scoreCollect = 0

  override def executeSignalOperation(graphEditor: GraphEditor[Int, Any]) {
    targetIds.foreach { targetId =>
      graphEditor.sendSignal(computeSignal(targetId), targetId, Some(id))
    }
    lastSignalState = state
  }

  def edgeCount = targetIds.size

  override def toString = s"${this.getClass.getName}(state=$state)"

  def executeCollectOperation(graphEditor: GraphEditor[Int, Any]) {
  }

  override def addEdge(e: Edge[Int], graphEditor: GraphEditor[Int, Any]): Boolean = {
    targetIds.insert(e.targetId.asInstanceOf[Int])
  }

  override def removeEdge(targetId: Int, graphEditor: GraphEditor[Int, Any]): Boolean = throw new UnsupportedOperationException

  override def removeAllEdges(graphEditor: GraphEditor[Int, Any]): Int = throw new UnsupportedOperationException

  def afterInitialization(graphEditor: GraphEditor[Int, Any]) = {}

  def beforeRemoval(graphEditor: GraphEditor[Int, Any]) = {}
}
