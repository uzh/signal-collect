/*
 *  @author Philip Stutz
 *
 *  @author Francisco de Freitas
 *  @author Daniel Strebel
 *
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.worker

class WorkerOperationCounters(
    var collectOperationsExecuted: Long = 0l,
    var signalOperationsExecuted: Long = 0l,
    var verticesAdded: Long = 0l,
    var verticesRemoved: Long = 0l,
    var outgoingEdgesAdded: Long = 0l,
    var outgoingEdgesRemoved: Long = 0l,
    var signalSteps: Long = 0l,
    var collectSteps: Long = 0l,
    var heartbeatMessagesReceived: Long = 0l,
    var signalMessagesReceived: Long = 0l,
    var bulkSignalMessagesReceived: Long = 0l,
    var continueMessagesReceived: Long = 0l,
    var requestMessagesReceived: Long = 0l,
    var otherMessagesReceived: Long = 0) {
  // Resets operation counters but not messages received/sent counters.
  def resetOperationCounters {
    collectOperationsExecuted = 0l
    signalOperationsExecuted = 0l
    verticesAdded = 0l
    verticesRemoved = 0l
    outgoingEdgesAdded = 0l
    outgoingEdgesRemoved = 0l
    signalSteps = 0l
    collectSteps = 0l
  }
  // Only these messages are part of termination detection.
  def messagesReceived = signalMessagesReceived + bulkSignalMessagesReceived + requestMessagesReceived + otherMessagesReceived
}
