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
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.BulkSignalNoSourceIds
import akka.actor.ActorSystem

class SignalBulker[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag](size: Int) {
  private var itemCount = 0
  def numberOfItems = itemCount
  def isFull: Boolean = itemCount == size
  final val sourceIds = new Array[Id](size)
  final val targetIds = new Array[Id](size)
  final val signals = new Array[Signal](size)
  def addSignal(signal: Signal, targetId: Id, sourceId: Id) {
    signals(itemCount) = signal
    targetIds(itemCount) = targetId
    sourceIds(itemCount) = sourceId
    itemCount += 1
  }
  def addSignal(signal: Signal, targetId: Id) {
    signals(itemCount) = signal
    targetIds(itemCount) = targetId
    itemCount += 1
  }
  def clear {
    itemCount = 0
  }
}

final class BulkMessageBus[Id: ClassTag, Signal: ClassTag](
  val system: ActorSystem,
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  val mapper: VertexToWorkerMapper[Id],
  flushThreshold: Int,
  val withSourceIds: Boolean,
  val sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
  workerApiFactory: WorkerApiFactory[Id, Signal])
  extends AbstractMessageBus[Id, Signal] {

  override def reset {
    super.reset
    pendingSignals = 0
    var i = 0
    val bulkers = outgoingMessages.length
    while (i < bulkers) {
      outgoingMessages(i).clear
      i += 1
    }
  }

  protected var pendingSignals = 0

  lazy val workerApi = workerApiFactory.createInstance(workerProxies, mapper)

  val outgoingMessages: Array[SignalBulker[Id, Signal]] = new Array[SignalBulker[Id, Signal]](numberOfWorkers)
  for (i <- 0 until numberOfWorkers) {
    outgoingMessages(i) = new SignalBulker[Id, Signal](flushThreshold)
  }

  override def flush {
    if (pendingSignals > 0) {
      var workerId = 0
      while (workerId < numberOfWorkers) {
        val bulker = outgoingMessages(workerId)
        val signalCount = bulker.numberOfItems
        if (signalCount > 0) {
          val signalsCopy = new Array[Signal](signalCount)
          System.arraycopy(bulker.signals, 0, signalsCopy, 0, signalCount)
          val targetIdsCopy = new Array[Id](signalCount)
          System.arraycopy(bulker.targetIds, 0, targetIdsCopy, 0, signalCount)
          if (withSourceIds) {
            val sourceIdsCopy = new Array[Id](signalCount)
            System.arraycopy(bulker.sourceIds, 0, sourceIdsCopy, 0, signalCount)
            super.sendToWorker(workerId, BulkSignal[Id, Signal](
              signalsCopy,
              targetIdsCopy,
              sourceIdsCopy))
          } else {
            super.sendToWorker(workerId, BulkSignalNoSourceIds[Id, Signal](
              signalsCopy,
              targetIdsCopy))
          }
          outgoingMessages(workerId).clear
        }
        workerId += 1
      }
      pendingSignals = 0
    }
  }

  @inline override def sendSignal(signal: Signal, targetId: Id, sourceId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(targetId)
    val bulker = outgoingMessages(workerId)
    bulker.addSignal(signal, targetId, sourceId)
    pendingSignals += 1
    if (bulker.isFull) {
      pendingSignals -= bulker.numberOfItems
      if (withSourceIds) {
        super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.clone, bulker.targetIds.clone, bulker.sourceIds.clone))
      } else {
        super.sendToWorker(workerId, BulkSignalNoSourceIds[Id, Signal](bulker.signals.clone, bulker.targetIds.clone))
      }
      bulker.clear
    }
  }

  @inline override def sendSignal(signal: Signal, targetId: Id) {
    val workerId = mapper.getWorkerIdForVertexId(targetId)
    val bulker = outgoingMessages(workerId)
    bulker.addSignal(signal, targetId)
    pendingSignals += 1
    if (bulker.isFull) {
      pendingSignals -= bulker.numberOfItems
      if (withSourceIds) {
        super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.clone, bulker.targetIds.clone, bulker.sourceIds.clone))
      } else {
        super.sendToWorker(workerId, BulkSignalNoSourceIds[Id, Signal](bulker.signals.clone, bulker.targetIds.clone))
      }
      bulker.clear
    }
  }

  @inline override def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      if (sourceId.isDefined) {
        workerApi.processSignalWithSourceId(signal, targetId, sourceId.get)
      } else {
        workerApi.processSignalWithoutSourceId(signal, targetId)
      }
    } else {
      val workerId = mapper.getWorkerIdForVertexId(targetId)
      val bulker = outgoingMessages(workerId)
      if (sourceId.isDefined) {
        bulker.addSignal(signal, targetId, sourceId.get)
      } else {
        bulker.addSignal(signal, targetId)
      }
      pendingSignals += 1
      if (bulker.isFull) {
        pendingSignals -= bulker.numberOfItems
        if (withSourceIds) {
          super.sendToWorker(workerId, BulkSignal[Id, Signal](bulker.signals.clone, bulker.targetIds.clone, bulker.sourceIds.clone))
        } else {
          super.sendToWorker(workerId, BulkSignalNoSourceIds[Id, Signal](bulker.signals.clone, bulker.targetIds.clone))
        }
        bulker.clear
      }
    }
  }

}
