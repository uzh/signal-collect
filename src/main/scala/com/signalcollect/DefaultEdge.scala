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
import com.signalcollect.interfaces.SignalMessage

/**
 *  DefaultEdgeId uniquely identifies an edge in the graph.
 *
 *  @param sourceId source vertex id
 *  @param targetId target vertex id
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 */
class DefaultEdgeId[SourceId, TargetId](
  val sourceId: SourceId,
  val targetId: TargetId,
  val description: String = "") extends EdgeId[SourceId, TargetId] {
  def this(sourceId: SourceId, targetId: TargetId) = this(sourceId, targetId, "")
  override def equals(x: Any): Boolean = {
    x match {
      case otherId: EdgeId[_, _] =>
        this.sourceId.equals(otherId.sourceId) &&
          this.targetId.equals(otherId.targetId) &&
          this.description.equals(otherId.description)
      case other =>
        false
    }
  }
  override val hashCode = (sourceId, targetId, description).hashCode
  override def toString = {
    if (description.length > 0) {
      sourceId + " " + description + " " + targetId
    } else {
      sourceId + "->" + targetId
    }

  }
}

/**
 *  Edge that connects a source vertex with a target vertex.
 *  Users of the framework extend this class to implement a specific algorithm by defining a `signal` function.
 *
 *  @param sourceId source vertex id
 *  @param targetId target vertex id
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 */
abstract class DefaultEdge[SourceIdTypeParameter, TargetIdTypeParameter](
  sourceId: SourceIdTypeParameter,
  targetId: TargetIdTypeParameter,
  description: String = "") extends Edge {

  /** The type of the source vertex id. */
  type SourceId = SourceIdTypeParameter

  /** The type of the target vertex id. */
  type TargetId = TargetIdTypeParameter

  /** The type of signals that are sent along this edge. */
  type Signal = Any

  /** Called when the edge is attached to a source vertex */
  def onAttach(sourceVertex: SourceVertex, graphEditor: GraphEditor) = {}
  
  /**
   *  An edge id uniquely identifies an edge in the graph.
   */
  val id = new DefaultEdgeId(sourceId, targetId, description)

  /** The weight of this edge: 1.0 by default, can be overridden. */
  def weight: Double = 1

  /**
   *  The abstract `signal` function is algorithm specific and is implemented by a user of the framework.
   *  It calculates the signal that is sent from the source vertex to the target vertex.
   *
   *  @param sourceVertex The source vertex to which this edge is currently attached as an outgoing edge.
   *
   *  @return The signal that will be sent along this edge.
   */
  def signal(sourceVertex: SourceVertex): Signal

  /**
   *  The hash code of the target vertex id is cached to speed up signaling.
   */
  val cachedTargetIdHashCode = id.targetId.hashCode

  /**
   *  Function that gets called by the source vertex whenever this edge is supposed to send a signal.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  def executeSignalOperation(sourceVertex: Vertex, messageBus: MessageBus) {
    messageBus.sendToWorkerForVertexIdHash(SignalMessage(id, signal(sourceVertex.asInstanceOf[SourceVertex])), cachedTargetIdHashCode)
  }

}