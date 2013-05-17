/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

package com.signalcollect.messaging

import scala.reflect.ClassTag
import com.signalcollect.interfaces.BulkSignal
import com.signalcollect.interfaces.WorkerApiFactory
import com.signalcollect.interfaces.MessageBus
import java.util.concurrent.atomic.AtomicInteger
import com.signalcollect.GraphEditor

class ParallelSignalBulker[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
    protected val size: Int,
    protected val messageBus: MessageBus[Id, Signal],
    protected val workerId: Int) {
  protected final val writePermissionsGranted = new AtomicInteger(0)
  protected final val successfulWrites = new AtomicInteger(0)
  protected final val maxIndex = size - 1
  protected final val targetIds = new Array[Id](size)
  protected final val signals = new Array[Signal](size)
  def addSignal(signal: Signal, targetId: Id) {
    var itemIndex = writePermissionsGranted.getAndIncrement
    while (itemIndex > maxIndex) {
      //Buffer is full, need to flush.
      flushIfNecessary
      itemIndex = writePermissionsGranted.getAndIncrement
    }
    //We have permission to write to a valid index.
    signals(itemIndex) = signal
    targetIds(itemIndex) = targetId
    val written = successfulWrites.incrementAndGet
    if (written == size) {
      flushIfNecessary
    }
  }
  protected def flushIfNecessary {
    synchronized {
      // Only one thread can ever be in this part and flush.
      // TODO: Check if flush is necessary before flushing.
      val written = successfulWrites.get
      if (written >= size) {
        // Flush is necessary.
        messageBus.sendToWorker(workerId, BulkSignal[Id, Signal](signals.clone, targetIds.clone, null.asInstanceOf[Array[Id]]))
        writePermissionsGranted.set(0)
        successfulWrites.set(0)
      }
    }
  }
  //WARNING: This is not thread-safe!
  def flush {
    val written = successfulWrites.get
    val pendingWrites = writePermissionsGranted.get
    require(written == pendingWrites)
    if (written > 0) {
      // Flush is necessary.
      messageBus.sendToWorker(workerId, BulkSignal[Id, Signal](signals.slice(0, written), targetIds.slice(0, written), null.asInstanceOf[Array[Id]]))
      writePermissionsGranted.set(0)
      successfulWrites.set(0)
    }
  }
  //WARNING: This is not thread-safe!
  def reset {
    val written = successfulWrites.get
    val pendingWrites = writePermissionsGranted.get
    require(written == pendingWrites)
    writePermissionsGranted.set(0)
    successfulWrites.set(0)
  }

}

/**
 * Does not support sending source ids.
 */
class ParallelBulkMessageBus[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val numberOfWorkers: Int,
  val numberOfNodes: Int,
  flushThreshold: Int,
  val sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
  workerApiFactory: WorkerApiFactory)
    extends AbstractMessageBus[Id, Signal] {

  var pendingSinceLastFlush = new AtomicInteger(0)

  override def reset {
    super.reset
    outgoingMessages foreach (_.reset)
  }

  lazy val workerApi = workerApiFactory.createInstance[Id, Signal](workerProxies, mapper)

  val outgoingMessages = new Array[ParallelSignalBulker[Id, Signal]](numberOfWorkers).par
  for (workerId <- 0 until numberOfWorkers) {
    outgoingMessages(workerId) = new ParallelSignalBulker[Id, Signal](flushThreshold, this, workerId)
  }

  //WARNING: This method is not thread-safe!
  override def flush {
    if (pendingSinceLastFlush.get > 0) {
      outgoingMessages foreach (_.flush)
      pendingSinceLastFlush.set(0)
    }
  }

  override def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean = false) {
    if (blocking) {
      // Use proxy.
      workerApi.processSignal(signal, targetId, sourceId)
    } else {
      pendingSinceLastFlush.incrementAndGet
      val workerId = mapper.getWorkerIdForVertexId(targetId)
      val bulker = outgoingMessages(workerId)
      bulker.addSignal(signal, targetId)
    }
  }

}