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
abstract class MemoryEfficientDataFlowVertex[State, GraphSignalType: ClassTag](
  val id: Int,
  var state: State) extends Vertex[Int, State, Int, GraphSignalType] {

  type OutgoingSignalType

  def collect(signal: GraphSignalType): State

  def computeSignal(targetId: Int): GraphSignalType

  var lastSignalState: State = null.asInstanceOf[State]

  def setState(s: State) {
    state = s
  }

  def targetIds: Traversable[Int] = {
    new Traversable[Int] {
      def foreach[U](f: Int => U) {
        _targetIds.foreach(f(_))
      }
    }
  }

  protected var _targetIds: SplayIntSet = new MemoryEfficientSplayIntSet

  def deliverSignalWithSourceId(signal: GraphSignalType, sourceId: Int, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    deliverSignalWithoutSourceId(signal, graphEditor)
  }

  def deliverSignalWithoutSourceId(signal: GraphSignalType, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    setState(collect(signal))
    true
  }

  /**
   * We always collect on delivery.
   */
  def scoreCollect = 0

  override def executeSignalOperation(graphEditor: GraphEditor[Int, GraphSignalType]) {
    _targetIds.foreach { targetId =>
      graphEditor.sendSignal(computeSignal(targetId), targetId, id)
    }
    lastSignalState = state
  }

  def edgeCount = _targetIds.size

  override def toString = s"${this.getClass.getSimpleName}(state=$state)"

  def executeCollectOperation(graphEditor: GraphEditor[Int, GraphSignalType]) {
  }

  override def addEdge(e: Edge[Int], graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    _targetIds.insert(e.targetId.asInstanceOf[Int])
  }

  override def removeEdge(targetId: Int, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = throw new UnsupportedOperationException

  override def removeAllEdges(graphEditor: GraphEditor[Int, GraphSignalType]): Int = throw new UnsupportedOperationException

  def afterInitialization(graphEditor: GraphEditor[Int, GraphSignalType]) = {}

  def beforeRemoval(graphEditor: GraphEditor[Int, GraphSignalType]) = {}
}
