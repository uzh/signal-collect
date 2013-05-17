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

import scala.reflect.ClassTag
import com.signalcollect.interfaces.BulkSignal
import com.signalcollect.interfaces.WorkerApiFactory
import com.signalcollect.interfaces.MessageBus

class SignalBulker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](size: Int) {
  private var itemCount = 0
  def numberOfItems = itemCount
  def isFull: Boolean = itemCount == size
  final val sourceIds = new Array[Id](size)
  final val targetIds = new Array[Id](size)
  final val signals = new Array[Signal](size)
  def addSignal(signal: Signal, targetId: Id, sourceId: Option[Id]) {
    signals(itemCount) = signal
    targetIds(itemCount) = targetId
    if (sourceId.isDefined) {
      sourceIds(itemCount) = sourceId.get
    }
    itemCount += 1
  }
  def clear {
    itemCount = 0
  }
}

class BulkMessageBus[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  flushThreshold: Int,
  val withSourceIds: Boolean,
  val sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
  workerApiFactory: WorkerApiFactory)
  extends AbstractMessageBus[Id, Signal] {

  override def reset {
    super.reset
    pendingSignals = 0
    outgoingMessages foreach (_.clear)
  }

  protected var pendingSignals = 0

  lazy val workerApi = workerApiFactory.createInstance[Id, Signal](workerProxies, mapper)

  val outgoingMessages: Array[SignalBulker[Id, Signal]] = new Array[SignalBulker[Id, Signal]](numberOfWorkers)
  for (i <- 0 until numberOfWorkers) {
    outgoingMessages(i) = new SignalBulker[Id, Signal](flushThreshold)
  }

  override def flush {
    if (pendingSignals > 0) {
      var workerId = 0
      while (workerId < numberOfWorkers) {
        //TODO: This could potentially be done in parallel. Evaluate.
        val bulker = outgoingMessages(workerId)
        val signalCount = bulker.numberOfItems
        if (signalCount > 0) {
          if (withSourceIds) {
            super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.slice(0, signalCount), bulker.targetIds.slice(0, signalCount), bulker.sourceIds.slice(0, signalCount)))
          } else {
            super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.slice(0, signalCount), bulker.targetIds.slice(0, signalCount), null.asInstanceOf[Array[Id]]))
          }
          outgoingMessages(workerId).clear
        }
        workerId += 1
      }
      pendingSignals = 0
    }
  }

  override def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.processSignal(signal, targetId, sourceId)
    } else {
      val workerId = mapper.getWorkerIdForVertexId(targetId)
      val bulker = outgoingMessages(workerId)
      if (withSourceIds) {
        bulker.addSignal(signal, targetId, sourceId)
      } else {
        bulker.addSignal(signal, targetId, None)
      }
      pendingSignals += 1
      if (bulker.isFull) {
        pendingSignals -= bulker.numberOfItems
        if (withSourceIds) {
          super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.clone, bulker.targetIds.clone, bulker.sourceIds.clone))
        } else {
          super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.clone, bulker.targetIds.clone, null.asInstanceOf[Array[Id]]))
        }
        bulker.clear
      }
    }
  }

}