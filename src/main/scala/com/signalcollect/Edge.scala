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
import com.signalcollect.interfaces.MessageBus

/**
 *  A DefaultEdgeId uniquely identifies an edge in the graph.
 *
 *  @param sourceId source vertex id
 *  @param targetId target vertex id
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 *  
 *  @author Philip Stutz
 *  @version 1.0
 *  @since 1.0
 */
case class DefaultEdgeId[SourceId, TargetId](sourceId: SourceId, targetId: TargetId, description: String = "") extends EdgeId[SourceId, TargetId]

/**
 *  An edge id uniquely identifies an edge in the graph.
 */
trait EdgeId[+SourceId, +TargetId] extends Serializable {
  def sourceId: SourceId
  def targetId: TargetId
  def description: String
}

trait Edge extends Serializable {

  /** The type of the source {@link Vertex} which can be found using {@link #sourceId}. */
  type SourceVertex <: Vertex

  /** The type of the source vertex id. */
  type SourceId

  /** The type of the target vertex id. */
  type TargetId

  /** The type of signals that are sent along this edge. */
  type Signal

  /** An edge id uniquely identifies an edge in the graph. */
  def id: EdgeId[SourceId, TargetId]

  /** Called when the edge is attached to a source vertex */
  def onAttach(sourceVertex: SourceVertex) = {}

  /** The weight of this edge: 1.0 by default, can be overridden. */
  def weight: Double = 1

  /** EdgeClassName(id=`edge id`) */
  override def toString = getClass.getSimpleName + "(id=" + id + ")"

  /** The edge hashCode is the hashCode of the id */
  override def hashCode = id.hashCode

  /** Two edges are equal if their ids are equal */
  override def equals(other: Any): Boolean = {
    if (other.isInstanceOf[Edge]) {
      id.equals(other.asInstanceOf[Edge].id)
    } else {
      false
    }
  }

  /**
   *  Function that gets called by the source vertex whenever this edge is supposed to send a signal.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  def executeSignalOperation(sourceVertex: Vertex, messageBus: MessageBus[Any])

}