/*
 *  @author Philip Stutz
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

trait ThrottlingBulkScheduler[Id, Signal] extends AkkaWorker[Id, Signal] {
  val batchSignalingSize = 10000

  override def scheduleOperations {
    if (!worker.vertexStore.toCollect.isEmpty) {
      worker.vertexStore.toCollect.process(worker.executeCollectOperationOfVertex(_))
    }
    if (!worker.vertexStore.toSignal.isEmpty) {
      worker.vertexStore.toSignal.process(worker.executeSignalOperationOfVertex(_), Some(batchSignalingSize))
      if (!worker.vertexStore.toSignal.isEmpty) {
        scheduleOperations
      }
    }
  }
}