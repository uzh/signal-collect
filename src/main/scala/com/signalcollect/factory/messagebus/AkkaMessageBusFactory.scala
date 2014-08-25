/*
 *  @author Philip Stutz
 *
 *  Copyright 2011 University of Zurich
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

package com.signalcollect.factory.messagebus

import scala.reflect.ClassTag
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.WorkerApiFactory
import com.signalcollect.messaging._
import com.signalcollect.interfaces.VertexToWorkerMapper
import akka.actor.ActorSystem

class AkkaMessageBusFactory[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag]
  extends MessageBusFactory[Id, Signal] {
  def createInstance(
    system: ActorSystem,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    mapper: VertexToWorkerMapper[Id],
    sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
    workerApiFactory: WorkerApiFactory[Id, Signal]): MessageBus[Id, Signal] = {
    new DefaultMessageBus[Id, Signal](
      system,
      numberOfWorkers,
      numberOfNodes,
      mapper,
      sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
      workerApiFactory)
  }
  override def toString = "AkkaMessageBusFactory"
}

/**
 * Stores outgoing messages until 'flushThreshold' messages are queued for a worker.
 * Combines messages for the same vertex using 'combiner'.
 */
class BulkAkkaMessageBusFactory[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag](
  flushThreshold: Int,
  withSourceIds: Boolean)
  extends MessageBusFactory[Id, Signal] {
  def createInstance(
    system: ActorSystem,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    mapper: VertexToWorkerMapper[Id],
    sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
    workerApiFactory: WorkerApiFactory[Id, Signal]): MessageBus[Id, Signal] = {
    new BulkMessageBus[Id, Signal](
      system,
      numberOfWorkers,
      numberOfNodes,
      mapper,
      flushThreshold,
      withSourceIds,
      sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
      workerApiFactory)
  }
  override def toString = "BulkAkkaMessageBusFactory"
}

class IntIdDoubleSignalMessageBusFactory(flushThreshold: Int)
  extends MessageBusFactory[Int, Double] {
  def createInstance(
    system: ActorSystem,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    mapper: VertexToWorkerMapper[Int],
    sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
    workerApiFactory: WorkerApiFactory[Int, Double]): MessageBus[Int, Double] = {
    new IntIdDoubleSignalMessageBus(
      system,
      numberOfWorkers,
      numberOfNodes,
      mapper,
      flushThreshold,
      sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
      workerApiFactory)
  }
  override def toString = "CombiningMessageBusFactory"
}

