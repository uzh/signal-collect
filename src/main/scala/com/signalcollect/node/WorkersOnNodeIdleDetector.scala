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

package com.signalcollect.node

import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.BulkStatus
import com.signalcollect.interfaces.WorkerStatus

class WorkersOnNodeIdleDetector(
  val nodeId: Int,
  val workersOnNode: Int,
  val messageBus: MessageBus[_, _]) {

  val workerStatus = new Array[WorkerStatus](workersOnNode)
  val isWorkerIdle = new Array[Boolean](workersOnNode)
  val workerStatusAlreadyForwarded = new Array[Boolean](workersOnNode)

  var numberOfIdleWorkers = 0
  var numberOfStatsThatNeedForwarding = 0
  var allIdleReported = false

  def receivedWorkerStatus(w: WorkerStatus) {
    val arrayIndex = w.workerId % workersOnNode
    if (isWorkerIdle(arrayIndex)) {
      if (!w.isIdle) {
        numberOfIdleWorkers -= 1
      }
    } else {
      if (w.isIdle) {
        numberOfIdleWorkers += 1
      }
    }
    if (workerStatusAlreadyForwarded(arrayIndex) || workerStatus(arrayIndex) == null) {
      // Only increase if there was no message there or if the message that will be replaced had already been forwarded.
      numberOfStatsThatNeedForwarding += 1
    }
    workerStatus(arrayIndex) = w
    isWorkerIdle(arrayIndex) = w.isIdle
    workerStatusAlreadyForwarded(arrayIndex) = false

    val nodeIsIdle = (numberOfIdleWorkers == workersOnNode)
    if (nodeIsIdle || allIdleReported) {
      val workerStats = new Array[WorkerStatus](numberOfStatsThatNeedForwarding)
      var i = 0
      var workerStatsIndex = 0
      while (i < workersOnNode) {
        if (!workerStatusAlreadyForwarded(i)) {
          val status = workerStatus(i)
          if (status != null) {
            workerStats(workerStatsIndex) = status
            workerStatsIndex += 1
          }
        }
        workerStatusAlreadyForwarded(i) = true
        i += 1
      }
      val bulkStatus = BulkStatus(nodeId, workerStats)
      messageBus.sendToNodeUncounted(nodeId / 2, bulkStatus)
      numberOfStatsThatNeedForwarding = 0
      allIdleReported = nodeIsIdle
    }
  }

}
