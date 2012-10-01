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

import com.signalcollect._

// Some edge ids that get sent around will be incomplete, by having one or both ids set to 'null'.
case class EdgeId(val sourceId: Any, val targetId: Any) {
  def withTargetId(t: Any): EdgeId = EdgeId(sourceId, t)
  def withSourceId(s: Any): EdgeId = EdgeId(s, targetId)
  def removeTargetId: EdgeId = EdgeId(sourceId, null)
  def removeSourceId: EdgeId = EdgeId(null, targetId)
}

case class SignalMessage[@specialized Signal](val signal: Signal, val edgeId: EdgeId)

// Convergence/pause detection
case class WorkerStatus(
  workerId: Int,
  isIdle: Boolean,
  isPaused: Boolean,
  messagesSent: Array[Int],
  messagesReceived: Long)

case class WorkerStatistics(
    messagesSent: Array[Long],
    messagesReceived: Long = 0l,
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
      { // Merges the sent messages arrays.
        if (messagesSent == null) other.messagesSent
        else if (other.messagesSent == null) messagesSent
        else if (messagesSent == null && other.messagesSent == null) null
        else {
          val merged = new Array[Long](messagesSent.length)
          var i = 0
          while (i < messagesSent.length) {
            merged(i) = messagesSent(i) + other.messagesSent(i)
            i += 1
          }
          merged
        }
      },
      messagesReceived + other.messagesReceived,
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
      "# collect operations executed" + "\t\t" + collectOperationsExecuted + "\n" +
      "# signal operations executed" + "\t\t" + signalOperationsExecuted + "\n" +
      "# vertices (added/removed)" + "\t\t" + numberOfVertices + " (" + verticesAdded + "/" + verticesRemoved + ")\n" +
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
