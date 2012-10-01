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

import java.util.HashMap
import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.interfaces.EdgeId
import scala.collection.mutable.IndexedSeq

abstract class AbstractVertex[Id, State] extends Vertex[Id, State] {

  /**
   * hashCode is cached for better performance
   */
  override lazy val hashCode = id.hashCode  // Lazy to prevent premature initialization when using Java API.

  def process(message: SignalMessage[_]) = {}

  def afterInitialization(graphEditor: GraphEditor) = {}

  /**
   * Access to the outgoing edges is required for some calculations and for executing the signal operations.
   * It is a map so we can support fast edge removals.
   */
  var outgoingEdges = new HashMap[Any, Edge[_]]()

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
  def addEdge(edge: Edge[_], graphEditor: GraphEditor): Boolean = {
    if (outgoingEdges.get(edge.targetId) == null) {
      edgesModifiedSinceSignalOperation = true
      edgesModifiedSinceCollectOperation = true
      outgoingEdges.put(edge.targetId, edge.asInstanceOf[Edge[_]])
      edge.onAttach(this, graphEditor: GraphEditor)
      true
    } else {
      false
    }
  }

  /**
   * Removes an outgoing {@link Edge} from this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def removeEdge(targetId: Any, graphEditor: GraphEditor): Boolean = {
    val outgoingEdge = outgoingEdges.get(targetId)
    if (outgoingEdge != null) {
      edgesModifiedSinceSignalOperation = true
      edgesModifiedSinceCollectOperation = true
      outgoingEdges.remove(targetId)
      true
    } else {
      false
    }
  }

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}.
   * @return returns the number of {@link Edge}s that were removed.
   */
  def removeAllEdges(graphEditor: GraphEditor): Int = {
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
  def executeSignalOperation(graphEditor: GraphEditor) {
    edgesModifiedSinceSignalOperation = false
    lastSignalState = Some(state)
    doSignal(graphEditor)
  }

  def doSignal(graphEditor: GraphEditor) {
    // Faster than Scala foreach.
    var i = outgoingEdges.values.iterator
    while (i.hasNext) {
      val outgoingEdge = i.next
      outgoingEdge.executeSignalOperation(this, graphEditor)
    }
  }

  /**
   * Adds the buffered signals for that vertex and executes the {@link #collect} method on this vertex.
   * @see #collect
   * @param signals Buffered Signals for this vertex
   */
  def executeCollectOperation(signals: IndexedSeq[SignalMessage[_]], graphEditor: GraphEditor) {
    edgesModifiedSinceCollectOperation = false
  }

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect(signals: IndexedSeq[SignalMessage[_]]): Double = {
    if (!signals.isEmpty) {
      1.0
    } else if (edgesModifiedSinceCollectOperation) {
      1.0
    } else {
      0.0
    }
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
        case noStateOrStateChanged               => 1
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
  def beforeRemoval(graphEditor: GraphEditor) = {}

}