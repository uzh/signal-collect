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

import scala.collection.JavaConversions.mapAsScalaMap

import com.signalcollect.interfaces.Inspectable

abstract class AbstractVertex[Id, State] extends Vertex[Id, State] with Inspectable[Id, State] {

  /**
   * hashCode is cached for better performance
   */
  override lazy val hashCode = id.hashCode // Lazy to prevent premature initialization when using Java API.

  def afterInitialization(graphEditor: GraphEditor[Any, Any]) = {}

  /**
   * Access to the outgoing edges is required for some calculations and for executing the signal operations.
   * It is a map so we can support fast edge removals.
   *
   *  Currently a Java HashMap is used as the implementation, but we will replace it with a more specialized
   *  implementation in a future release.
   */
  var outgoingEdges: collection.mutable.Map[Any, Edge[_]] = new java.util.HashMap[Any, Edge[_]](0)

  /** The edges that this vertex is connected to. */
  def edges: Traversable[Edge[_]] = outgoingEdges.values

  /** The state of this vertex when it last signaled. */
  var lastSignalState: Option[State] = None

  /** Keeps track if edges get modified so we know we should signal again */
  var edgesModifiedSinceSignalOperation = false

  /** Keeps track if edges get modified so we know we should collect again */
  var edgesModifiedSinceCollectOperation = false

  /**
   * Adds a new outgoing `Edge`
   *
   * @param e the edge to be added.
   */
  def addEdge(edge: Edge[_], graphEditor: GraphEditor[Any, Any]): Boolean = {
    outgoingEdges.get(edge.targetId) match {
      case None =>
        edgesModifiedSinceSignalOperation = true
        edgesModifiedSinceCollectOperation = true
        outgoingEdges.put(edge.targetId, edge)
        edge.onAttach(this, graphEditor)
        true
      case Some(edge) =>
        false
    }
  }

  /**
   * Removes an outgoing {@link Edge} from this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def removeEdge(targetId: Any, graphEditor: GraphEditor[Any, Any]): Boolean = {
    val outgoingEdge = outgoingEdges.get(targetId)
    outgoingEdge match {
      case None =>
        false
      case Some(edge) =>
        edgesModifiedSinceSignalOperation = true
        edgesModifiedSinceCollectOperation = true
        outgoingEdges.remove(targetId)
        true
    }
  }

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}.
   * @return returns the number of {@link Edge}s that were removed.
   */
  def removeAllEdges(graphEditor: GraphEditor[Any, Any]): Int = {
    val edgesRemoved = outgoingEdges.size
    for (outgoingEdge <- outgoingEdges.keys) {
      removeEdge(outgoingEdge, graphEditor)
    }
    edgesRemoved
  }

  /**
   * This method tells this Vertex to execute the signal operation
   * on all its outgoing edges. This method is going to be
   * called by the Signal/Collect framework during its execution (i.e. the
   * Worker implementation.
   *
   * @see Worker
   * @see Edge#executeSignalOperation
   */
  def executeSignalOperation(graphEditor: GraphEditor[Any, Any]) {
    edgesModifiedSinceSignalOperation = false
    lastSignalState = Some(state)
    doSignal(graphEditor)
  }

  def doSignal(graphEditor: GraphEditor[Any, Any]) {
    outgoingEdges.values foreach (_.executeSignalOperation(this, graphEditor))
  }

  /**
   *  Function that gets called by the framework whenever this vertex is supposed to collect new signals.
   *
   *  @param graphEditor an instance of GraphEditor which can be used by this vertex to interact with the graph.
   */
  def executeCollectOperation(graphEditor: GraphEditor[Any, Any]) {
    edgesModifiedSinceCollectOperation = false
  }

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value. The meaning of this value depends on the thresholds set in {@link ComputeGraph#execute}.
   */
  def scoreSignal: Double = {
    if (edgesModifiedSinceSignalOperation) {
      1
    } else {
      lastSignalState match {
        case Some(oldState) if oldState == state => 0
        case noStateOrStateChanged => 1
      }
    }
  }

  /** Returns the number of outgoing edges of this [com.signalcollect.interfaces.Vertex] */
  def edgeCount = outgoingEdges.size

  /**
   * Returns "VertexClassName(id=ID, state=STATE)"
   */
  override def toString: String = {
    this.getClass.getSimpleName + "(id=" + id + ", state=" + state + ")"
  }

  /**
   *  This method gets called by the framework before the vertex gets removed.
   */
  def beforeRemoval(graphEditor: GraphEditor[Any, Any]) = {}

  /**
   * Returns the ids of the target vertices of outgoing edges of the vertex.
   */
  def getTargetIdsOfOutgoingEdges: Iterable[Any] = {
    outgoingEdges.values map (_.targetId)
  }

}
