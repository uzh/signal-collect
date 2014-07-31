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

package com.signalcollect.factory.worker

import scala.reflect.ClassTag
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.worker.AkkaWorker
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.interfaces.ExistingVertexHandlerFactory
import com.signalcollect.interfaces.UndeliverableSignalHandlerFactory
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.worker._

/**
 *  The default Akka worker implementation.
 */
class AkkaWorkerFactory[Id: ClassTag, Signal: ClassTag] extends WorkerFactory[Id, Signal] {
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
    supportBlockingGraphModificationsInVertex: Boolean): AkkaWorker[Id, Signal] = {
    val idClass = implicitly[ClassTag[Id]].runtimeClass.asInstanceOf[Class[Id]]
    val signalClass = implicitly[ClassTag[Signal]].runtimeClass.asInstanceOf[Class[Signal]]
    val int = classOf[Int]
    val long = classOf[Long]
    val float = classOf[Float]
    val double = classOf[Double]
    val any = classOf[Any]
    val worker: AkkaWorker[Id, Signal] = {
      if (idClass == int && signalClass == int) {
        new IntIntAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Int, Int]],
          mapperFactory.asInstanceOf[MapperFactory[Int]],
          storageFactory.asInstanceOf[StorageFactory[Int, Int]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Int, Int]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Int, Int]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Int, Int]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Int, Int]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == int && signalClass == long) {
        new IntLongAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Int, Long]],
          mapperFactory.asInstanceOf[MapperFactory[Int]],
          storageFactory.asInstanceOf[StorageFactory[Int, Long]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Int, Long]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Int, Long]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Int, Long]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Int, Long]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == int && signalClass == float) {
        new IntFloatAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Int, Float]],
          mapperFactory.asInstanceOf[MapperFactory[Int]],
          storageFactory.asInstanceOf[StorageFactory[Int, Float]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Int, Float]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Int, Float]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Int, Float]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Int, Float]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == int && signalClass == double) {
        new IntDoubleAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Int, Double]],
          mapperFactory.asInstanceOf[MapperFactory[Int]],
          storageFactory.asInstanceOf[StorageFactory[Int, Double]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Int, Double]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Int, Double]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Int, Double]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Int, Double]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == int && signalClass == any) {
        new IntAnyAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Int, Any]],
          mapperFactory.asInstanceOf[MapperFactory[Int]],
          storageFactory.asInstanceOf[StorageFactory[Int, Any]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Int, Any]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Int, Any]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Int, Any]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Int, Any]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == long && signalClass == int) {
        new LongIntAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Long, Int]],
          mapperFactory.asInstanceOf[MapperFactory[Long]],
          storageFactory.asInstanceOf[StorageFactory[Long, Int]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Long, Int]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Long, Int]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Long, Int]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Long, Int]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == long && signalClass == long) {
        new LongLongAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Long, Long]],
          mapperFactory.asInstanceOf[MapperFactory[Long]],
          storageFactory.asInstanceOf[StorageFactory[Long, Long]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Long, Long]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Long, Long]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Long, Long]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Long, Long]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == long && signalClass == float) {
        new LongFloatAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Long, Float]],
          mapperFactory.asInstanceOf[MapperFactory[Long]],
          storageFactory.asInstanceOf[StorageFactory[Long, Float]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Long, Float]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Long, Float]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Long, Float]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Long, Float]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == long && signalClass == double) {
        new LongDoubleAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Long, Double]],
          mapperFactory.asInstanceOf[MapperFactory[Long]],
          storageFactory.asInstanceOf[StorageFactory[Long, Double]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Long, Double]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Long, Double]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Long, Double]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Long, Double]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == long && signalClass == any) {
        new LongAnyAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Long, Any]],
          mapperFactory.asInstanceOf[MapperFactory[Long]],
          storageFactory.asInstanceOf[StorageFactory[Long, Any]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Long, Any]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Long, Any]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Long, Any]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Long, Any]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == any && signalClass == int) {
        new AnyIntAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Any, Int]],
          mapperFactory.asInstanceOf[MapperFactory[Any]],
          storageFactory.asInstanceOf[StorageFactory[Any, Int]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Any, Int]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Any, Int]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Any, Int]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Any, Int]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == any && signalClass == long) {
        new AnyLongAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Any, Long]],
          mapperFactory.asInstanceOf[MapperFactory[Any]],
          storageFactory.asInstanceOf[StorageFactory[Any, Long]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Any, Long]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Any, Long]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Any, Long]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Any, Long]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == any && signalClass == float) {
        new AnyFloatAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Any, Float]],
          mapperFactory.asInstanceOf[MapperFactory[Any]],
          storageFactory.asInstanceOf[StorageFactory[Any, Float]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Any, Float]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Any, Float]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Any, Float]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Any, Float]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else if (idClass == any && signalClass == double) {
        new AnyDoubleAkkaWorker(
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory.asInstanceOf[MessageBusFactory[Any, Double]],
          mapperFactory.asInstanceOf[MapperFactory[Any]],
          storageFactory.asInstanceOf[StorageFactory[Any, Double]],
          schedulerFactory.asInstanceOf[SchedulerFactory[Any, Double]],
          existingVertexHandlerFactory.asInstanceOf[ExistingVertexHandlerFactory[Any, Double]],
          undeliverableSignalHandlerFactory.asInstanceOf[UndeliverableSignalHandlerFactory[Any, Double]],
          edgeAddedToNonExistentVertexHandlerFactory.asInstanceOf[EdgeAddedToNonExistentVertexHandlerFactory[Any, Double]],
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex).asInstanceOf[AkkaWorker[Id, Signal]]
      } else {
        new AkkaWorker[Id, Signal](
          workerId,
          numberOfWorkers,
          numberOfNodes,
          messageBusFactory,
          mapperFactory,
          storageFactory,
          schedulerFactory,
          existingVertexHandlerFactory,
          undeliverableSignalHandlerFactory,
          edgeAddedToNonExistentVertexHandlerFactory,
          heartbeatIntervalInMilliseconds,
          eagerIdleDetection,
          throttlingEnabled,
          supportBlockingGraphModificationsInVertex)
      }
    }
    worker
  }
  override def toString: String = "AkkaWorkerFactory"
}
