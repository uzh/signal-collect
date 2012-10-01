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

import com.signalcollect.interfaces.SignalMessage
import collection.immutable.Map
import collection.Iterable
import com.signalcollect.interfaces.EdgeId
import scala.collection.mutable.IndexedSeq

/**
 *  This trait represents the framework's view of a vertex.
 *
 *  @author Philip Stutz
 */
trait Vertex[Id, State] extends Serializable {

  override def hashCode = id.hashCode

  /**
   * Two vertices are equal if their ids are equal.
   */
  override def equals(other: Any): Boolean =
    other match {
      case v: Vertex[_, _] => v.id == id
      case _               => false
    }

  /**
   * A vertex id uniquely defines a vertex in the graph.
   */
  def id: Id
  def state: State
  def setState(s: State)

  /**
   *  Adds a new outgoing `Edge` to this `Vertex`.
   *  @param e the edge to be added.
   */
  def addEdge(e: Edge[_], graphEditor: GraphEditor): Boolean

  /**
   *  Removes an outgoing `Edge` from this `Vertex`.
   *  @param edgeId the edge id to be removed
   *  @return returns if an edge was removed
   */
  def removeEdge(targetId: Any, graphEditor: GraphEditor): Boolean

  /**
   *  Removes all outgoing `Edge`s from this `Vertex`, returns the number of edges that were removed.
   */
  def removeAllEdges(graphEditor: GraphEditor): Int

  /**
   *  This method tells this `Vertex` to execute the signal operation on all its outgoing
   *  Edges. This method is going to be called by the framework during its execution (i.e. the
   *  `Worker` implementations).
   */
  def executeSignalOperation(graphEditor: GraphEditor)

  /**
   *  Tells this vertex to execute the `collect` method.
   *
   *  @param signals the new signals that this vertex has received since the `executeCollectOperation` method was called last.
   */
  def executeCollectOperation(signals: IndexedSeq[SignalMessage[_]], graphEditor: GraphEditor)

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value.
   */
  def scoreSignal: Double

  /**
   * This method is used by the framework in order to decide if the collect operation
   * should be executed.
   *
   * @param signals the signals that have not yet been delivered to the `executeCollectOperation` method yet.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect(signals: IndexedSeq[SignalMessage[_]]): Double

  /**
   *  @return the number of outgoing edges of this `Vertex`
   */
  def edgeCount: Int

  /**
   *  This method gets called by the framework after the vertex has been fully initialized.
   */
  def afterInitialization(graphEditor: GraphEditor)

  /**
   *  This method gets called by the framework before the vertex gets removed.
   */
  def beforeRemoval(graphEditor: GraphEditor)
  
}