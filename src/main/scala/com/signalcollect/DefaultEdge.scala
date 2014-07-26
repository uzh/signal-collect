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

import com.signalcollect.interfaces.EdgeId
import akka.event.Logging
import com.signalcollect.configuration.ActorSystemRegistry
import scala.annotation.elidable
import scala.annotation.elidable._
import com.signalcollect.interfaces.SignalMessageWithSourceId

/**
 *  Edge that connects a source vertex with a target vertex.
 *  Users of the framework extend this class to implement a specific algorithm by defining a `signal` function.
 *
 *  @param targetId target vertex id
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 */
abstract class DefaultEdge[TargetId](val targetId: TargetId) extends Edge[TargetId] {

  /** The type of signals that are sent along this edge. */
  type Signal = Any

  /**
   * Calls to debug level logging are by default disregarded by the compiler and do not get executed.
   * To enable them decrease the default S/C "-Xelide-below" compiler parameter from "INFO" to "ALL".
   *
   * Note: this logging has no memory overhead for a reference.
   */
  @elidable(FINEST) def debug(message: String) {
    val system = ActorSystemRegistry.retrieve("SignalCollect")
    system match {
      case Some(s) => Logging.getLogger(s, this).debug(message)
      case None =>
    }
  }

  /**
   * Info level logging is by default enabled and very expensive.
   * To disable increase the default S/C "-Xelide-below" compiler parameter from "INFO" to "WARNING".
   *
   * Note: this logging has no memory overhead for a reference.
   */
  @elidable(INFO) def info(message: String) {
    val system = ActorSystemRegistry.retrieve("SignalCollect")
    system match {
      case Some(s) => Logging.getLogger(s, this).info(message)
      case None =>
    }
  }

  /**
   * Warning level logging is by default enabled and very expensive.
   * To disable increase the default S/C "-Xelide-below" compiler parameter from "INFO" to "SEVERE".
   *
   * Note: this logging has no memory overhead for a reference.
   */
  @elidable(WARNING) def warning(message: String) {
    val system = ActorSystemRegistry.retrieve("SignalCollect")
    system match {
      case Some(s) => Logging.getLogger(s, this).warning(message)
      case None =>
    }
  }

  var source: Source = _

  /**
   *  An edge id uniquely identifies an edge in the graph.
   */
  def id = EdgeId(sourceId, targetId)

  /** The weight of this edge: 1.0 by default, can be overridden. */
  def weight: Double = 1

  /**
   *  The abstract `signal` function is algorithm specific and is implemented by a user of the framework.
   *  It calculates the signal that is sent from the source vertex to the target vertex.
   *
   *  @param sourceVertex The source vertex to which this edge is currently attached as an outgoing edge.
   *
   *  @return The signal that will be sent along this edge.
   */
  def signal: Signal

  /**
   *  The hash code of the target vertex id is cached to speed up signaling.
   */
  val cachedTargetIdHashCode = targetId.hashCode

  /**
   *  Function that gets called by the source vertex whenever this edge is supposed to send a signal.
   *
   *  @param sourceVertex The source vertex of this edge.
   *
   *  @param messageBus an instance of MessageBus which can be used by this edge to interact with the graph.
   */
  def executeSignalOperation(sourceVertex: Vertex[_, _, _, _], graphEditor: GraphEditor[Any, Any]) {
    graphEditor.sendToWorkerForVertexIdHash(SignalMessageWithSourceId(targetId, sourceId, signal), cachedTargetIdHashCode)
  }

  /** Called when the edge is attached to a source vertex */
  def onAttach(sourceVertex: Vertex[_, _, _, _], graphEditor: GraphEditor[Any, Any]) = {
    source = sourceVertex.asInstanceOf[Source]
  }

}