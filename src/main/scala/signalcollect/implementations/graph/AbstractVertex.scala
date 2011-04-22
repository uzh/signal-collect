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

package signalcollect.implementations.graph

import signalcollect.implementations.messaging.AbstractMessageRecipient
import signalcollect.interfaces.Queue._
import scala.collection.mutable.ListBuffer
import signalcollect.implementations.messaging.MultiQueue
import signalcollect.implementations.messaging.AbstractMessageRecipient
import scala.collection.mutable.Buffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer
import signalcollect.interfaces._
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

abstract class AbstractVertex[@specialized IdType, @specialized StateType](messageInboxFactory: QueueFactory = Queue.linkedBlockingQueueFactory) extends AbstractMessageRecipient[Any](messageInboxFactory) with Vertex[IdType, StateType] with Serializable {

  protected def process(message: Any) = {}

  protected def afterInitialization = {}
  
  /** Setter for {@link #_messageBus} over which this vertex is communicating with its outgoing edges. */
  def setMessageBus(mb: MessageBus[Any, Any]) {
    messageBus = mb
    afterInitialization
  }

  /** The state of this vertex when it last signaled. */
  protected var lastSignalState: Option[StateType] = None

  /** The message bus over which this vertex is communicating with its outgoing edges. */
  @transient protected var messageBus: MessageBus[Any, Any] = _ // null instead of None (Option) because it simplifies the API. Framework is required to set this before calling {@link #executeSignalOperation}

  /** Keeps track if edges get added so this vertex remembers to signal for those */
  protected var outgoingEdgeAddedSinceSignalOperation = false

  /**
   * Adds a new outgoing {@link Edge} to this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def addOutgoingEdge(e: Edge[_, _]) {
    val newEdge = e.asInstanceOf[Edge[IdType, _]]
    if (!outgoingEdges.contains(newEdge.id)) {
      processNewOutgoingEdge(newEdge)
    }
  }

  protected def processNewOutgoingEdge(e: Edge[IdType, _]) {
    outgoingEdgeAddedSinceSignalOperation = true
    e.setSource(this)
    outgoingEdges.put(e.id, e)
  }

  /**
   * Removes an outgoing {@link Edge} from this {@link FrameworkVertex}.
   * @param e the edge to be added.
   */
  def removeOutgoingEdge(edgeId: (Any, Any, String)): Boolean = {
    val castEdgeId = edgeId.asInstanceOf[(IdType, Any, String)]
    if (outgoingEdges.contains(castEdgeId)) {
      val outgoingEdge = outgoingEdges.get(castEdgeId).get
      processRemoveOutgoingEdge(outgoingEdge)
      true
    } else {
      false
    }
  }

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}.
   * @return returns the number of {@link Edge}s that were removed.
   */
  def removeAllOutgoingEdges {
    outgoingEdges.keys foreach (removeOutgoingEdge(_))
  }

  protected def processRemoveOutgoingEdge(e: Edge[IdType, _]) {
    messageBus.sendToWorkerForIdHash(CommandRemoveIncomingEdge(e.id), e.targetHashCode)
    outgoingEdges.remove(e.id)
  }

  /**
   * Informs this vertex that there is a new incoming edge.
   * @param edgeId the id of the new incoming edge
   */
  def addIncomingEdge(edgeId: (Any, Any, String)) {}

  /**
   * Informs this vertex that an incoming edge was removed.
   * @param edgeId the id of the incoming edge that was removed
   */
  def removeIncomingEdge(edgeId: (Any, Any, String)): Option[Boolean] = {
    None
  }

  /**
   * Access to the outgoing edges is required for some calculations and for executing the signal operations
   */
  protected val outgoingEdges: Map[(IdType, Any, String), Edge[IdType, _]] = HashMap[(IdType, Any, String), Edge[IdType, _]]()

  /**
   * This method tells this {@link FrameworkVertex} to execute the signal operation
   * on all its outgoing {@Edge}s. This method is going to be
   * called by the SignalCollect framework during its execution (i.e. the
   * {@link Worker} implementations).
   *
   * @see Worker
   * @see Edge#executeSignalOperation
   */
  def executeSignalOperation {
    outgoingEdgeAddedSinceSignalOperation = false
    lastSignalState = Some(state)
    outgoingEdges.values.foreach(_.executeSignalOperation(messageBus))
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  def executeCollectOperation {
    processInbox
    state = collect
  }

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect: Double = {
    if (messageInbox.isEmpty)
      0
    else
      1
  }

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value. The meaning of this value depends on the thresholds set in {@link ComputeGraph#execute}.
   */
  def scoreSignal: Double = {
    if (outgoingEdgeAddedSinceSignalOperation) {
      1
    } else {
      lastSignalState match {
        case None => 1
        case Some(oldState) => {
          if (oldState.equals(state))
            0
          else
            1
        }
      }
    }
  }

  /** Optionally returns the number of outgoing edges of this [signalcollect.interfaces.Vertex] */
  def outgoingEdgeCount = Some(outgoingEdges.size)

  /** Optionally returns the number of incoming edges of this [signalcollect.interfaces.Vertex] */
  def incomingEdgeCount: Option[Int] = None

  /**
   * Returns "VertexClassName> Id: vertexId, State: vertexState"
   */
  override def toString: String = {
    this.getClass.getSimpleName + "> Id: " + id + ", State: " + state
  }

}