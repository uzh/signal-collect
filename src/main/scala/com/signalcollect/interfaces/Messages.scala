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

// algorithm-specific message
case class Signal[+SourceIdType, +TargetIdType, +SignalType](sourceId: SourceIdType, targetId: TargetIdType, signal: SignalType)

// signal sender id used to indicate that a signal was sent from an external source
object EXTERNAL { override val toString = "EXTERNAL" }

// rpc request
case class WorkerRequest(command: Worker => Unit)
// rpc reply
case class WorkerReply(workerId: Int, result: Any)

// stalling detection
case class WorkerStatus(
  workerId: Int,
  isIdle: Boolean,
  isPaused: Boolean,
  messagesSent: Long,
  messagesReceived: Long)

case class WorkerStatistics(
  messagesReceived: Long = 0l,
  messagesSent: Long = 0l,
  collectOperationsExecuted: Long = 0l,
  signalOperationsExecuted: Long = 0l,
  numberOfVertices: Long = 0l,
  verticesAdded: Long = 0l,
  verticesRemoved: Long = 0l,
  numberOfOutgoingEdges: Long = 0l,
  outgoingEdgesAdded: Long = 0l,
  outgoingEdgesRemoved: Long = 0l) {
  def +(other: WorkerStatistics): WorkerStatistics = {
    WorkerStatistics(
      messagesReceived + other.messagesReceived,
      messagesSent + other.messagesSent,
      collectOperationsExecuted + other.collectOperationsExecuted,
      signalOperationsExecuted + other.signalOperationsExecuted,
      numberOfVertices + other.numberOfVertices,
      verticesAdded + other.verticesAdded,
      verticesRemoved + other.verticesRemoved,
      numberOfOutgoingEdges + other.numberOfOutgoingEdges,
      outgoingEdgesAdded + other.outgoingEdgesAdded,
      outgoingEdgesRemoved + other.outgoingEdgesRemoved)
  }
  override def toString: String = {
    "messages received" + "\t" + messagesReceived + "\n" +
      "messages sent" + "\t" + messagesSent + "\n" +
      "# collect operations executed" + "\t" + collectOperationsExecuted + "\n" +
      "# signal operations executed" + "\t" + signalOperationsExecuted + "\n" +
      "# vertices (added/removed)" + "\t" + numberOfVertices + " (" + verticesAdded + "/" + verticesRemoved + ")\n" +
      "# outgoing edges  (added/removed)" + "\t" + numberOfOutgoingEdges + " (" + outgoingEdgesAdded + "/" + outgoingEdgesRemoved + ")"
  }
}

sealed trait LogMessage {
  def msg: Any
  def from: Any
}

case class Debug(msg: Any, from: Any) extends LogMessage
case class Config(msg: Any, from: Any) extends LogMessage
case class Info(msg: Any, from: Any) extends LogMessage
case class Warning(msg: Any, from: Any) extends LogMessage
case class Severe(msg: Any, from: Any) extends LogMessage
