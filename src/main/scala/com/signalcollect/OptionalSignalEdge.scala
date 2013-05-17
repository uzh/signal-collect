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

/**
 *  OptionalSignalEdge is an edge implementation that  that requires the
 *  signal function to return an instance of type `Option` and only signals
 *  if this signal has type `Some`.
 *
 *  @param sourceId id of this edge's source vertex
 *  @param targetId id of this edges's target vertex
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 */
abstract class OptionalSignalEdge[TargetIdType](targetId: TargetIdType) extends DefaultEdge(targetId) {

  /**
   *  Calculates the new signal and potentially sends it. If the return value of the `signal` function is of type `Some`,
   *  then the encapsulated value is sent. If it is of type `None`, then nothing is sent.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  override def executeSignalOperation(sourceVertex: Vertex[_, _], graphEditor: GraphEditor[Any, Any]) {
    val optionalSignal = signal.asInstanceOf[Option[_]]
    if (optionalSignal.isDefined) {
      graphEditor.sendToWorkerForVertexIdHash(SignalMessage(targetId, Some(sourceId), optionalSignal.get), cachedTargetIdHashCode)
    }
  }
}