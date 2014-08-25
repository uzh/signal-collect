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

package com.signalcollect.messaging

import com.signalcollect.interfaces.BulkSignalNoSourceIds
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.WorkerApiFactory
import com.signalcollect.util.IntDoubleHashMap

import akka.actor.ActorSystem

class CombingingDoubleBulkerNoIds {
  @inline final def numberOfItems = map.size
  final val map = new IntDoubleHashMap(initialSize = 8, rehashFraction = 0.4f)
  def addSignal(signal: Double, targetId: Int) {
    map.addToValueForKey(targetId, signal)
  }
  def getBulkSignalAndClear: BulkSignalNoSourceIds[Int, Double] = {
    val length = numberOfItems
    val targetIds = new Array[Int](length)
    val signals = new Array[Double](length)
    var i = 0
    map.process { (id, signal) =>
      targetIds(i) = id
      signals(i) = signal
      i += 1
    }
    val bulkSignal = new BulkSignalNoSourceIds(signals, targetIds)
    bulkSignal
  }
  def clear {
    map.clear
  }
}

/**
 * Example message bus with a combiner for Int IDs that sums Double signals.
 * Vertex ID 0 is not supported when using this message bus.
 */
final class IntIdDoubleSignalMessageBus(
  val system: ActorSystem,
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  val mapper: VertexToWorkerMapper[Int],
  val flushThreshold: Int,
  val sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
  val workerApiFactory: WorkerApiFactory[Int, Double])
  extends AbstractMessageBus[Int, Double] {

  lazy val workerApi = workerApiFactory.createInstance(workerProxies, mapper)
  val outgoingMessages: Array[CombingingDoubleBulkerNoIds] = new Array[CombingingDoubleBulkerNoIds](numberOfWorkers)
  for (i <- 0 until numberOfWorkers) {
    outgoingMessages(i) = new CombingingDoubleBulkerNoIds
  }

  protected var pendingSignals = 0

  override def sendSignal(signal: Double, targetId: Int) {
    val workerId = mapper.getWorkerIdForVertexId(targetId)
    val bulker = outgoingMessages(workerId)
    bulker.addSignal(signal, targetId)
    pendingSignals += 1
    if (pendingSignals >= flushThreshold) {
      flush
    }
  }

  override def sendSignal(signal: Double, targetId: Int, sourceId: Option[Int], blocking: Boolean = false) {
    throw new Exception(s"Non-optimized messaging for this bulk message bus, this should never be called: signal=$signal targetId=$targetId")
  }

  override def flush {
    if (pendingSignals > 0) {
      var workerId = 0
      while (workerId < numberOfWorkers) {
        val bulker = outgoingMessages(workerId)
        val signalCount = bulker.numberOfItems
        if (signalCount > 0) {
          val bulkSignal = bulker.getBulkSignalAndClear
          super.sendToWorker(workerId, bulkSignal)
        }
        workerId += 1
      }
      pendingSignals = 0
    }
  }

  override def reset {
    super.reset
    pendingSignals = 0
    outgoingMessages.foreach(_.clear)
  }

}
