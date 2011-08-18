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
 * This class represents the framework's view of a vertex.
 *
 * This trait requires to always be extended by something that implements {@link Vertex}
 * which gives us access to methods and fields in {@link Vertex}.
 */
trait Vertex extends Serializable {

  type Id
  type State
  type Signal
  
  /**
   * By default it is assumed that a vertex can receive signals of any type.
   * This type can be overridden to set an upper bound for the types of signals
   * that this vertex can receive. This occasionally allows for better  allows for more elegant implementations.
   */

  /**
   * Vertices are assigned to worker threads that are each responsible for a part of the graph.
   * We use a hash function on the vertex ids for the mapping of vertices to workers.
   */
  override def hashCode = this.id.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case v: Vertex => v.id == id
      case _ => false
    }
  }

  /** @return the identifier of this {@link FrameworkVertex}. */
  def id: Id

  /** @return the object that stores the current state for this {@link Vertex}. */
  var state: State

  /**
   * Adds a new outgoing {@link Edge} to this {@link Vertex}.
   * @param e the edge to be added.
   */
  def addOutgoingEdge(e: Edge): Boolean

  /**
   * Removes an outgoing {@link Edge} from this {@link Vertex}.
   * @param e the edge to be added..
   * @return returns if an edge was removed
   */
  def removeOutgoingEdge(edgeId: EdgeId[_, _]): Boolean

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}, returns the number of edges that were removed.
   */
  def removeAllOutgoingEdges: Int

  /**
   * This method tells this {@link FrameworkVertex} to execute the signal operation
   * on all its outgoing {@Edge}s. This method is going to be
   * called by the SignalCollect framework during its execution (i.e. the
   * {@link Worker} implementations).
   *
   * @see Worker
   * @see Edge#executeSignalOperation
   */
  def executeSignalOperation(messageBus: MessageBus[Any])

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   * @param signals the new signals that have not been transferred to the vertex jet.
   */
  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any])

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value. The meaning of this value depends on the thresholds set in {@link ComputeGraph#execute}.
   */
  def scoreSignal: Double

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect(signals: Iterable[SignalMessage[_, _, _]]): Double

  /** @return the number of outgoing edges of this {@link Vertex} */
  def outgoingEdgeCount: Int
  
  /**
   * Returns the ids of all vertices to which this vertex has an outgoing edge
   */
  def getVertexIdsOfSuccessors: Iterable[_]

  /**
   * Returns the ids of all vertices from which this vertex has an incoming edge, optional.
   */
  def getVertexIdsOfPredecessors: Option[Iterable[_]]

  /**
   * Returns the most recent signal sent via the edge with the id @edgeId. None if this function is not
   * supported or if there is no such signal.
   */
  def getMostRecentSignal(id: EdgeId[_, _]): Option[_]
  
  /**
   * Returns all outgoing edges if this operation is supported by the vertex
   */
  def getOutgoingEdges: Option[Iterable[Edge]] = None
  
  /** This method gets called by the framework after the vertex has been fully initialized. */
  def afterInitialization(messageBus: MessageBus[Any])

}