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

package signalcollect.interfaces

// algorithm-specific message
case class Signal[+SourceIdType, +TargetIdType, +SignalType](sourceId: SourceIdType, targetId: TargetIdType, signal: SignalType)

// signal sender id used to indicate that a signal was sent from an external source
object EXTERNAL { override val toString = "EXTERNAL" }

// signal target id used to indicate that a signal should be delivered to all vertices
object ALL { override val toString = "ALL" }

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

case class WorkerStats(
  var messagesReceived: Long = 0l,
  var collectOperationsExecuted: Long = 0l,
  var signalOperationsExecuted: Long = 0l,
  var verticesAdded: Long = 0l,
  var verticesRemoved: Long = 0l,
  var outgoingEdgesAdded: Long = 0l,
  var outgoingEdgesRemoved: Long = 0l) {
  def +(other: WorkerStats): WorkerStats = {
    WorkerStats(
      messagesReceived + other.messagesReceived,
      collectOperationsExecuted + other.collectOperationsExecuted,
      signalOperationsExecuted + other.signalOperationsExecuted,
      verticesAdded + other.verticesAdded,
      verticesRemoved + other.verticesRemoved,
      outgoingEdgesAdded + other.outgoingEdgesAdded,
      outgoingEdgesRemoved + other.outgoingEdgesRemoved)
  }
}