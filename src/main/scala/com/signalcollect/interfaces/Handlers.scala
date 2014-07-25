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

package com.signalcollect.interfaces

import com.signalcollect.Vertex
import com.signalcollect.GraphEditor
import com.signalcollect.Edge

trait ExistingVertexHandler[Id, Signal] {

  /**
   *  Sets the function that can intervene when a vertex with the same ID is added
   *  repeatedly. The new vertex will be thrown out for sure, but some of its
   *  information might be added to the existing vertex.
   *
   *  @note By default the addition of a vertex is ignored if an existing vertex has the same ID.
   */
  def mergeVertices(existing: Vertex[Id, _, Id, Signal], failedVertexAddition: Vertex[Id, _, Id, Signal], ge: GraphEditor[Id, Signal])

}

trait UndeliverableSignalHandler[@specialized(Int, Long) Id, Signal] {

  /**
   *  Sets the function that handles signals that could not be delivered to a vertex.
   *
   *  @note By default an exception is thrown when a signal is not deliverable. The handler function
   *  		receives the signal and an instance of GraphEditor as parameters in order to take some
   *  		action that handles this case.
   */
  def vertexForSignalNotFound(signal: Signal, inexistentTargetId: Id, senderId: Option[Id], graphEditor: GraphEditor[Id, Signal])

}

trait EdgeAddedToNonExistentVertexHandler[@specialized(Int, Long) Id, Signal] {

  /**
   *  Sets the handler that gets triggered, when the vertex to which an edge should be added does not exist.
   *  Optionally returns the vertex that should be created and to whioch the edge can then be added.
   *
   *  @note By default an exception is thrown when an edge cannot be added. The handler function
   *  		receives the edge, the id of the vertex that does not exist and an instance of GraphEditor as parameters in order to
   *    		potentially create a vertex to which the edge should be added.
   */
  def handleImpossibleEdgeAddition(edge: Edge[Id], vertexId: Id): Option[Vertex[Id, _, Id, Signal]]

}
