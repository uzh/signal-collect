/*
 *  @author Philip Stutz
 *  @author Francisco de Freitas
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

package com.signalcollect.interfaces

import com.signalcollect.Vertex
import com.signalcollect.Edge

case object GetNodes

case class NodeReady(nodeId: Int)

case class Request[ProxiedClass](
  command: ProxiedClass => Any,
  returnResult: Boolean = false,
  incrementorForReply: MessageBus[_, _] => Unit // Answers go to temporary actor. This allows to assign the send count to the real recipient.
  )

case class Heartbeat(maySignal: Boolean)

// Some edge ids that get sent around will be incomplete, by having one or both ids set to 'null'.
case class EdgeId[@specialized(Int, Long) Id](val sourceId: Id, val targetId: Id) {
  def withTargetId(t: Id): EdgeId[Id] = EdgeId(sourceId, t)
  def withSourceId(s: Id): EdgeId[Id] = EdgeId(s, targetId)
  override def toString: String = s"${sourceId} -> ${targetId}"
}

case class AddVertex[Id, State, GraphIdUpperBound, GraphSignalUpperBound](
  v: Vertex[Id, State, GraphIdUpperBound, GraphSignalUpperBound])

case class AddEdge[@specialized(Int, Long) SourceId, @specialized(Int, Long) TargetId](
  sourceVertexId: SourceId,
  e: Edge[TargetId])

case class BulkSignal[@specialized(Int, Long) Id, Signal](
  val signals: Array[Signal],
  val targetIds: Array[Id],
  val sourceIds: Array[Id])

case class BulkSignalNoSourceIds[@specialized(Int, Long) Id, Signal](
  val signals: Array[Signal],
  val targetIds: Array[Id])

case class SignalMessageWithSourceId[@specialized(Int, Long) Id, Signal](
  val targetId: Id,
  val sourceId: Id,
  val signal: Signal)

case class SignalMessageWithoutSourceId[@specialized(Int, Long) Id, Signal](
  val targetId: Id,
  val signal: Signal)  
  
// Convergence/pause detection
case class WorkerStatus(
  workerId: Int,
  timeStamp: Long,
  isIdle: Boolean,
  isPaused: Boolean,
  messagesSent: SentMessagesStats,
  messagesReceived: Long)

// Convergence/pause detection
case class NodeStatus(
  nodeId: Int,
  messagesSent: SentMessagesStats,
  messagesReceived: Long)

case class SentMessagesStats(
  workers: Array[Int],
  nodes: Array[Int],
  coordinator: Int,
  other: Int) {
  def sumRelevant: Long = 0L + workers.sum + nodes.sum + coordinator
}

case class WorkerStatistics(
  workerId: Option[Int] = None,
  toSignalSize: Long = 0L,
  toCollectSize: Long = 0L,
  collectOperationsExecuted: Long = 0L,
  signalOperationsExecuted: Long = 0L,
  numberOfVertices: Long = 0L,
  verticesAdded: Long = 0L,
  verticesRemoved: Long = 0L,
  numberOfOutgoingEdges: Long = 0L,
  outgoingEdgesAdded: Long = 0L,
  outgoingEdgesRemoved: Long = 0L,
  receiveTimeoutMessagesReceived: Long = 0L,
  heartbeatMessagesReceived: Long = 0L,
  signalMessagesReceived: Long = 0L,
  bulkSignalMessagesReceived: Long = 0L,
  continueMessagesReceived: Long = 0L,
  requestMessagesReceived: Long = 0L,
  otherMessagesReceived: Long = 0L,
  messagesSentToWorkers: Long = 0L,
  messagesSentToNodes: Long = 0L,
  messagesSentToCoordinator: Long = 0L,
  messagesSentToOthers: Long = 0L) {
  def +(other: WorkerStatistics): WorkerStatistics = {
    WorkerStatistics(
      None,
      toSignalSize + other.toSignalSize,
      toCollectSize + other.toCollectSize,
      collectOperationsExecuted + other.collectOperationsExecuted,
      signalOperationsExecuted + other.signalOperationsExecuted,
      numberOfVertices + other.numberOfVertices,
      verticesAdded + other.verticesAdded,
      verticesRemoved + other.verticesRemoved,
      numberOfOutgoingEdges + other.numberOfOutgoingEdges,
      outgoingEdgesAdded + other.outgoingEdgesAdded,
      outgoingEdgesRemoved + other.outgoingEdgesRemoved,
      receiveTimeoutMessagesReceived + other.receiveTimeoutMessagesReceived,
      heartbeatMessagesReceived + other.heartbeatMessagesReceived,
      signalMessagesReceived + other.signalMessagesReceived,
      bulkSignalMessagesReceived + other.bulkSignalMessagesReceived,
      continueMessagesReceived + other.continueMessagesReceived,
      requestMessagesReceived + other.requestMessagesReceived,
      otherMessagesReceived + other.otherMessagesReceived,
      messagesSentToWorkers + other.messagesSentToWorkers,
      messagesSentToNodes + other.messagesSentToNodes,
      messagesSentToCoordinator + other.messagesSentToCoordinator,
      messagesSentToOthers + other.messagesSentToOthers)
  }

  def toSimpleString: String = {
    "# collect operations\t" + collectOperationsExecuted + "\n" +
      "# signal operations\t" + signalOperationsExecuted + "\n" +
      "# vertices (add/remove)\t" + numberOfVertices + " (" + verticesAdded + "/" + verticesRemoved + ")\n" +
      "# edges (add/remove)\t" + numberOfOutgoingEdges + " (" + outgoingEdgesAdded + "/" + outgoingEdgesRemoved + ")"
  }

}

case class NodeStatistics(
  nodeId: Option[Int] = None,
  os: String = "",
  runtime_mem_total: Long = 0L,
  runtime_mem_max: Long = 0L,
  runtime_mem_free: Long = 0L,
  runtime_cores: Long = 0L,
  jmx_committed_vms: Long = 0L,
  jmx_mem_free: Long = 0L,
  jmx_mem_total: Long = 0L,
  jmx_swap_free: Long = 0L,
  jmx_swap_total: Long = 0L,
  jmx_process_load: Double = 0.0,
  jmx_process_time: Double = 0.0,
  jmx_system_load: Double = 0.0) {
  def +(other: NodeStatistics): NodeStatistics = {
    NodeStatistics(
      None,
      os + other.os,
      runtime_mem_total + other.runtime_mem_total,
      runtime_mem_max + other.runtime_mem_max,
      runtime_mem_free + other.runtime_mem_free,
      runtime_cores + other.runtime_cores,
      jmx_committed_vms + other.jmx_committed_vms,
      jmx_mem_free + other.jmx_mem_free,
      jmx_mem_total + other.jmx_mem_total,
      jmx_swap_free + other.jmx_swap_free,
      jmx_swap_total + other.jmx_swap_total,
      (jmx_process_load + other.jmx_process_load) / 2,
      jmx_process_time + other.jmx_process_time,
      (jmx_system_load + other.jmx_system_load) / 2)
  }
}
