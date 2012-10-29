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

package com.signalcollect.messaging

import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.interfaces.BulkSignal
import scala.reflect.ClassTag

class SignalBulker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](size: Int) {
  private var itemCount = 0
  def numberOfItems = itemCount
  def isFull: Boolean = itemCount == size
  final val targetIds = new Array[Id](size)
  final val signals = new Array[Signal](size)
  def addSignal(targetId: Id, signal: Signal) {
    targetIds(itemCount) = targetId
    signals(itemCount) = signal
    itemCount += 1
  }
}

class BulkMessageBus[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](val numberOfWorkers: Int, flushThreshold: Int)
    extends AbstractMessageBus[Id, Signal] {

  val outgoingMessages: Array[SignalBulker[Id, Signal]] = new Array[SignalBulker[Id, Signal]](numberOfWorkers)
  for (i <- 0 until numberOfWorkers) {
    outgoingMessages(i) = new SignalBulker[Id, Signal](flushThreshold)
  }

  override def flush {
    var workerId = 0
    while (workerId < numberOfWorkers) {
      val bulker = outgoingMessages(workerId)
      val signalCount = bulker.numberOfItems
      if (signalCount > 0) {
        super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.targetIds.slice(0, signalCount), bulker.signals.slice(0, signalCount)))
        outgoingMessages(workerId) = new SignalBulker[Id, Signal](flushThreshold)
      }
      workerId += 1
    }
  }

  override def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.sendSignal(signal, targetId, sourceId)
    } else {
      val workerId = mapper.getWorkerIdForVertexId(targetId)
      val bulker = outgoingMessages(workerId)
      bulker.addSignal(targetId, signal)
      if (bulker.isFull) {
        super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.targetIds, bulker.signals))
        outgoingMessages(workerId) = new SignalBulker[Id, Signal](flushThreshold)
      }
    }
  }

}