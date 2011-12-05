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
import collection.immutable.Map
import collection.Iterable

/**
 *  This trait represents the framework's view of a vertex.
 *
 *  @author Philip Stutz
 */
trait Vertex extends Serializable {

  /**
   *  By default it is assumed that a vertex can receive signals of any type.
   *  This type can be overridden to set an upper bound for the types of signals
   *  that this vertex can receive. This occasionally allows for better  allows for more elegant implementations.
   */
  @specialized type Signal
  @specialized type Id
  @specialized type State

  /**
   *  Vertices are assigned to worker threads that are each responsible for a part of the graph.
   *  We use a hash function on the vertex ids for the mapping of vertices to workers.
   */
  override def hashCode = this.id.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case v: Vertex => v.id == id
      case _ => false
    }
  }

  /**
   *  @return the identifier of this `Vertex`.
   */
  def id: Id

  /**
   *  @return the object that stores the current state for this `Vertex`.
   */
  var state: State

  /**
   *  Adds a new outgoing `Edge` to this `Vertex`.
   *  @param e the edge to be added.
   */
  def addOutgoingEdge(e: Edge): Boolean

  /**
   *  Removes an outgoing `Edge` from this `Vertex`.
   *  @param edgeId the edge id to be removed
   *  @return returns if an edge was removed
   */
  def removeOutgoingEdge(edgeId: EdgeId[_, _]): Boolean

  /**
   *  Adds a new incoming `Edge` to this `Vertex`.
   *  @param e the edge to be added.
   */
  def addIncomingEdge(e: Edge): Boolean

  /**
   *  Removes incoming `Edge` from this `Vertex`.
   *  @param edgeId of the edge to be removed.
   */
  def removeIncomingEdge(edgeId: EdgeId[_, _]): Boolean
  
  /**
   *  Removes all outgoing `Edge`s from this `Vertex`, returns the number of edges that were removed.
   */
  def removeAllOutgoingEdges: Int

  /**
   *  This method tells this `Vertex` to execute the signal operation on all its outgoing
   *  Edges. This method is going to be called by the framework during its execution (i.e. the
   *  `Worker` implementations).
   */
  def executeSignalOperation(messageBus: MessageBus[Any])

  /**
   *  Tells this vertex to execute the `collect` method.
   *
   *  @param signals the new signals that this vertex has received since the `executeCollectOperation` method was called last.
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any])

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
  def scoreCollect(signals: Iterable[SignalMessage[_, _, _]]): Double

  /**
   *  @return the number of outgoing edges of this `Vertex`
   */
  def outgoingEdgeCount: Int

  /**
   *  @return ids of all vertices to which this vertex currently has an outgoing edge
   */
  def getVertexIdsOfSuccessors: Iterable[_]

  /**
   *  @return ids of vertices that currently have an outgoing edge to to this vertex. `None` if this operation is not
   *  supported.
   */
  def getVertexIdsOfPredecessors: Option[Iterable[Any]]

  /**
   *  @return the most recent signal sent via the edge with the id `edgeId`. `None` if this operation is not
   *  supported or if there is no such signal.
   */
  def getMostRecentSignal(id: EdgeId[_, _]): Option[_]

  /**
   *  @return all outgoing edges if this operation is supported by the vertex, `None` if this operation is not
   *  supported or if there is no such signal.
   */
  def getOutgoingEdges: Option[Iterable[Edge]]

  /**
   *  @return A map with edge ids as keys and edges as values. Optional, but supported by the default implementations
   */
  def getOutgoingEdgeMap: Option[Map[EdgeId[Id, _], Edge]]

  /**
   *  This method gets called by the framework after the vertex has been fully initialized.
   */
  def afterInitialization(messageBus: MessageBus[Any])

  /**
   *  This method gets called by the framework before the vertex gets removed.
   */
  def beforeRemoval(messageBus: MessageBus[Any])

}