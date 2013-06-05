/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.scheduler

import com.signalcollect.Vertex
import com.signalcollect.interfaces.Scheduler
import com.signalcollect.interfaces.Worker

class ThroughputScheduler[Id](w: Worker[Id, _]) extends Scheduler[Id](w) {
  val batchSignalingSize = 10000

  override def executeOperations {
    if (!worker.vertexStore.toCollect.isEmpty) {
      worker.vertexStore.toCollect.process(worker.executeCollectOperationOfVertex(_))
    }
    if (!worker.vertexStore.toSignal.isEmpty) {
      worker.vertexStore.toSignal.process(worker.executeSignalOperationOfVertex(_), Some(batchSignalingSize))
      worker.messageBusFlushed = false
    }
  }

  override def handleCollectOnDelivery(v: Vertex[Id, _]) {
    worker.vertexStore.toSignal.put(v)
  }

}
