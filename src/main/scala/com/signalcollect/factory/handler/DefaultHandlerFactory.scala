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

package com.signalcollect.factory.handler

import com.signalcollect._
import com.signalcollect.interfaces._

class DefaultExistingVertexHandlerFactory[Id, Signal] extends ExistingVertexHandlerFactory[Id, Signal] {
  def createInstance: ExistingVertexHandler[Id, Signal] =
    new DefaultExistingVertexHandler[Id, Signal]
  override def toString = "DefaultExistingVertexHandlerFactory"
}

class DefaultExistingVertexHandler[Id, Signal] extends ExistingVertexHandler[Id, Signal] {
  def mergeVertices(existing: Vertex[Id, _, Id, Signal], failedVertexAddition: Vertex[Id, _, Id, Signal], ge: GraphEditor[Id, Signal]) {
    // Do nothing, the second vertex with the same id is silently discarded.
  }
}

class DefaultUndeliverableSignalHandlerFactory[@specialized(Int, Long) Id, Signal] extends UndeliverableSignalHandlerFactory[Id, Signal] {
  def createInstance: UndeliverableSignalHandler[Id, Signal] =
    new DefaultUndeliverableSignalHandler[Id, Signal]
  override def toString = "DefaultUndeliverableSignalHandlerFactory"
}

class DefaultUndeliverableSignalHandler[@specialized(Int, Long) Id, Signal] extends UndeliverableSignalHandler[Id, Signal] {
  def vertexForSignalNotFound(s: Signal, inexistentTargetId: Id, senderId: Option[Id], ge: GraphEditor[Id, Signal]) {
    throw new Exception(s"Undeliverable signal: $s from $senderId could not be delivered to $inexistentTargetId, because no vertex with that id exists..")
  }
}

class DefaultEdgeAddedToNonExistentVertexHandlerFactory[@specialized(Int, Long) Id, Signal] extends EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal] {
  def createInstance: EdgeAddedToNonExistentVertexHandler[Id, Signal] =
    new DefaultEdgeAddedToNonExistentVertexHandler[Id, Signal]
  override def toString = "DefaultEdgeAddedToNonExistentVertexHandlerFactory"
}

class DefaultEdgeAddedToNonExistentVertexHandler[@specialized(Int, Long) Id, Signal] extends EdgeAddedToNonExistentVertexHandler[Id, Signal] {
  def handleImpossibleEdgeAddition(edge: Edge[Id], vertexId: Id): Option[Vertex[Id, _, Id, Signal]] = {
    throw new Exception(
      s"Could not add edge: ${edge.getClass.getSimpleName}(id = $vertexId -> ${edge.targetId}), because vertex with id $vertexId does not exist.")
  }
}
