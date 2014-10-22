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

import com.signalcollect.util.SplayIntSet
import com.signalcollect.util.IntHashMap
import scala.reflect.ClassTag
import com.signalcollect.util.MemoryEfficientSplayIntSet

/**
 * A memory efficient implementation of a data graph vertex
 * with an Int id and a memory efficient edge representation
 * that can only represent edges without additional attributes
 * that point to a vertex with an Int id.
 * The signal function is the same for all edges and defined in
 * the 'computeSignal' function.
 */
abstract class MemoryEfficientDataGraphVertex[State, IncomingSignalType: ClassTag, GraphSignalType](
  val id: Int,
  var state: State) extends Vertex[Int, State, Int, GraphSignalType] {

  def collect: State

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

  val mostRecentSignalMap = new IntHashMap[IncomingSignalType](1, 0.85f)

  override def deliverSignalWithSourceId(signal: GraphSignalType, sourceId: Int, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    val s = signal.asInstanceOf[IncomingSignalType]
    mostRecentSignalMap.put(sourceId, s)
    false
  }

  override def deliverSignalWithoutSourceId(signal: GraphSignalType, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    throw new Exception(s"A data graph vertex requires the sender ID for signals. Vertex ${this.toString} just received signal ${signal} without a sender ID.")
  }

  override def executeSignalOperation(graphEditor: GraphEditor[Int, GraphSignalType]) {
    _targetIds.foreach { targetId =>
      graphEditor.sendSignal(computeSignal(targetId), targetId, id)
    }
    lastSignalState = state
  }

  def edgeCount = _targetIds.size

  override def toString = s"${this.getClass.getName}(state=$state)"

  def executeCollectOperation(graphEditor: GraphEditor[Int, GraphSignalType]) {
    setState(collect)
  }

  override def addEdge(e: Edge[Int], graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = {
    _targetIds.insert(e.targetId.asInstanceOf[Int])
  }

  override def removeEdge(targetId: Int, graphEditor: GraphEditor[Int, GraphSignalType]): Boolean = throw new UnsupportedOperationException

  override def removeAllEdges(graphEditor: GraphEditor[Int, GraphSignalType]): Int = throw new UnsupportedOperationException

  def afterInitialization(graphEditor: GraphEditor[Int, GraphSignalType]) = {}

  def beforeRemoval(graphEditor: GraphEditor[Int, GraphSignalType]) = {}
}
