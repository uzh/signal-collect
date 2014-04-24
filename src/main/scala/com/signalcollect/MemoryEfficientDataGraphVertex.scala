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

final class MemoryEfficientSplayIntSet extends SplayIntSet {
  final val overheadFraction = 0.05f
  final val maxNodeIntSetSize = 10000
}

/**
 * A memory efficient implementation of a data graph vertex
 * with an Int id and a memory efficient edge representation
 * that can only represent edges without additional attributes
 * that point to a vertex with an Int id.
 * The signal function is the same for all edges and defined in
 * the 'computeSignal' function.
 */
abstract class MemoryEfficientDataGraphVertex[State](
  val id: Int,
  var state: State) extends Vertex[Int, State] {

  def collect: State

  def computeSignal(targetId: Int): OutgoingSignalType

  type IncomingSignalType
  implicit def tag: ClassTag[IncomingSignalType]

  type OutgoingSignalType

  var lastSignalState: State = null.asInstanceOf[State]

  def setState(s: State) {
    state = s
  }

  protected var targetIds: SplayIntSet = new MemoryEfficientSplayIntSet

  val mostRecentSignalMap = new IntHashMap[IncomingSignalType](1, 0.85f)

  def deliverSignal(signal: Any, sourceId: Option[Any], graphEditor: GraphEditor[Any, Any]): Boolean = {
    val s = signal.asInstanceOf[IncomingSignalType]
    mostRecentSignalMap.put(sourceId.get.asInstanceOf[Int], s)
    false
  }

  override def executeSignalOperation(graphEditor: GraphEditor[Any, Any]) {
    targetIds.foreach { targetId =>
      graphEditor.sendSignal(computeSignal(targetId), targetId, Some(id))
    }
    lastSignalState = state
  }

  def edgeCount = targetIds.size

  override def toString = s"${this.getClass.getName}(state=$state)"

  def executeCollectOperation(graphEditor: GraphEditor[Any, Any]) {
    setState(collect)
  }

  override def addEdge(e: Edge[_], graphEditor: GraphEditor[Any, Any]): Boolean = {
    targetIds.insert(e.targetId.asInstanceOf[Int])
  }

  override def removeEdge(targetId: Any, graphEditor: GraphEditor[Any, Any]): Boolean = throw new UnsupportedOperationException

  override def removeAllEdges(graphEditor: GraphEditor[Any, Any]): Int = throw new UnsupportedOperationException

  def afterInitialization(graphEditor: GraphEditor[Any, Any]) = {}

  def beforeRemoval(graphEditor: GraphEditor[Any, Any]) = {}
}
