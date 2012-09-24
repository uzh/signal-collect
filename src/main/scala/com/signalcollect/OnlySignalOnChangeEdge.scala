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

import com.signalcollect.interfaces._

/**
 *  OnlySignalOnChangeEdge is an edge implementation that only signals
 *  when the signal has changed compared to the last signal sent.
 *  For some algorithms this can reduce the number of messages sent.
 *
 *  @param targetId id of this edges's target vertex
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 *
 *  @note Beware of modifying and signaling a referenced object, change detection fails in this case.
 */
abstract class OnlySignalOnChangeEdge[SourceIdType, TargetIdType](targetId: TargetIdType)
    extends DefaultEdge(targetId) {

  /** Last signal sent along this edge */
  var lastSignalSent: Option[Signal] = None

  /**
   *  Function that gets called by the source vertex whenever this edge is supposed to send a signal.
   *  Compared to the default implementation there is an additional check if the signal has changed
   *  and no signal is sent if the signal has not changed.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  override def executeSignalOperation(sourceVertex: Vertex[_, _], graphEditor: GraphEditor) {
    val newSignal = signal(sourceVertex)
    if (!lastSignalSent.isDefined || !lastSignalSent.get.equals(newSignal)) {
      graphEditor.sendToWorkerForVertexIdHash(SignalMessage(newSignal, senderEdgeId), cachedTargetIdHashCode)
      lastSignalSent = Some(newSignal)
    }
  }
}