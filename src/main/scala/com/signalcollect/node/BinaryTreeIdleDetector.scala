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

import scala.annotation.migration

import com.signalcollect.interfaces.BulkStatus
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.WorkerStatus

/**
 * A virtual binary tree is formed over all nodes.
 * Worker status messages for idle detection are propagated from the leaf nodes
 * up, with node 0 being the root. At each point we only propagate information that
 * can either result in an idle detection or that can shield the coordinator from
 * receiving information that could not result in an idle detection anyway.
 *
 * The purpose of this scheme is to maintain the low latency of eager idle detection,
 * but make it scale to many nodes.
 */
class BinaryTreeIdleDetector(
  val nodeId: Int,
  val numberOfNodes: Int,
  val workersPerNode: Int,
  val messageBus: MessageBus[_, _]) {

  var allIdleReported = false
  var allMostRecentWorkerStats = Map.empty[Int, WorkerStatus]
  var unreportedWorkerStats = Map.empty[Int, WorkerStatus]

  def computeNumberOfSubnodes(id: Int): Int = {
    if (id == 0) {
      numberOfNodes
    } else {
      if (1 + id * 2 < numberOfNodes) {
        val left = id * 2
        val right = 1 + id * 2
        2 + computeNumberOfSubnodes(left) + computeNumberOfSubnodes(right)
      } else if (id * 2 < numberOfNodes) {
        val left = id * 2
        1 + computeNumberOfSubnodes(left)
      } else {
        0
      }
    }
  }

  val numberOfWorkersUnderIdleDetection = computeNumberOfSubnodes(nodeId) * workersPerNode

  def receivedBulkStatus(b: BulkStatus) {
    b.fromWorkers.foreach { s =>
      unreportedWorkerStats += ((s.workerId, s))
      allMostRecentWorkerStats += ((s.workerId, s))
    }

    val allNewReportsIdle = b.fromWorkers.forall(_.isIdle)
    if (allIdleReported && allNewReportsIdle) {
      reportToParent(allIdle = true)
    } else if (allIdleReported && !allNewReportsIdle) {
      reportToParent(allIdle = false)
    } else if (!allIdleReported && !allNewReportsIdle) {
      // Do nothing.
    } else { // !allIdleReported && allNewReportsIdle
      // We have to check if all workers are idle now.
      if (allMostRecentWorkerStats.size == numberOfWorkersUnderIdleDetection) {
        // There's enough reports, potentially they're all idle. We need to verify.
        val allIdle = allMostRecentWorkerStats.values.forall(_.isIdle)
        if (allIdle) {
          reportToParent(allIdle = true)
        }
      } else {
        // Not enough reports for all to be idle. Do nothing.
      }
    }
  }

  def reportToParent(allIdle: Boolean) {
    val bulkStatus = BulkStatus(nodeId, unreportedWorkerStats.values.toArray)
    if (nodeId == 0) {
      if (allIdle) { // We only tell the coordinator if all of them are idle.
        messageBus.sendToCoordinatorUncounted(bulkStatus)
        unreportedWorkerStats = Map.empty
        allIdleReported = allIdle
      }
    } else {
      messageBus.sendToNodeUncounted(nodeId / 2, bulkStatus)
      unreportedWorkerStats = Map.empty
      allIdleReported = allIdle
    }
  }

}
