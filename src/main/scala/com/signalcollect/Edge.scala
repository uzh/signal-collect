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
 * This trait represents the framework's view of an edge.
 *
 *  @author Philip Stutz
 */
trait Edge[+TargetId] extends Serializable {

  type Source <: Vertex[_, _]

  /** An edge id uniquely identifies an edge in the graph. */
  def id: EdgeId[_]

  def sourceId: Any = source.id
  def targetId: TargetId
  def source: Source

  /** Called when the edge is attached to a source vertex */
  def onAttach(source: Vertex[_, _], graphEditor: GraphEditor[Any, Any])

  /** The weight of this edge. */
  def weight: Double

  /** EdgeClassName(id=`edge id`) */
  override def toString = getClass.getSimpleName + "(id=" + id + ")"

  /** The edge hashCode is the hashCode of the id */
  override def hashCode = id.hashCode

  /** Two edges are equal if their ids are equal */
  override def equals(other: Any): Boolean =
    other match {
      case e: Edge[_] => e.id == id
      case _          => false
    }

  /**
   *  Function that gets called by the source vertex whenever this edge is supposed to send a signal.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  def executeSignalOperation(sourceVertex: Vertex[_, _], graphEditor: GraphEditor[Any, Any])

}