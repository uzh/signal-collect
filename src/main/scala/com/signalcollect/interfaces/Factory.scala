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

package com.signalcollect.interfaces

import scala.reflect.ClassTag
import com.signalcollect.factory.workerapi.DefaultWorkerApiFactory
import akka.actor.ActorSystem
import com.signalcollect.worker.AkkaWorker

abstract class Factory extends Serializable {
  override def toString = this.getClass.getSimpleName
}

abstract class ExistingVertexHandlerFactory[Id, Signal] extends Factory {
  def createInstance: ExistingVertexHandler[Id, Signal]
}

abstract class UndeliverableSignalHandlerFactory[Id, Signal] extends Factory {
  def createInstance: UndeliverableSignalHandler[Id, Signal]
}

abstract class EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal] extends Factory {
  def createInstance: EdgeAddedToNonExistentVertexHandler[Id, Signal]
}

abstract class WorkerFactory[Id: ClassTag, Signal: ClassTag] extends Factory {
  def createInstance(
    workerId: Int,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    messageBusFactory: MessageBusFactory[Id, Signal],
    mapperFactory: MapperFactory[Id],
    storageFactory: StorageFactory[Id, Signal],
    schedulerFactory: SchedulerFactory[Id, Signal],
    existingVertexHandlerFactory: ExistingVertexHandlerFactory[Id, Signal],
    undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Id, Signal],
    edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal],
    heartbeatIntervalInMilliseconds: Int,
    eagerIdleDetection: Boolean,
    throttlingEnabled: Boolean,
    throttlingDuringLoadingEnabled: Boolean,
    supportBlockingGraphModificationsInVertex: Boolean): AkkaWorker[Id, Signal]
}

abstract class MessageBusFactory[Id: ClassTag, Signal: ClassTag] extends Factory {
  def createInstance(
    system: ActorSystem,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    mapper: VertexToWorkerMapper[Id],
    sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
    workerApiFactory: WorkerApiFactory[Id, Signal] = new DefaultWorkerApiFactory[Id, Signal]): MessageBus[Id, Signal]
}

abstract class MapperFactory[Id] extends Factory {
  def createInstance(numberOfNodes: Int, workersPerNode: Int): VertexToWorkerMapper[Id]
}

abstract class StorageFactory[Id, Signal] extends Factory {
  def createInstance: Storage[Id, Signal]
}

abstract class SchedulerFactory[Id, Signal] extends Factory {
  def createInstance(worker: Worker[Id, Signal]): Scheduler[Id, Signal]
}

abstract class WorkerApiFactory[Id, Signal] extends Factory {
  def createInstance(
    workerProxies: Array[WorkerApi[Id, Signal]],
    mapper: VertexToWorkerMapper[Id]): WorkerApi[Id, Signal]
}
