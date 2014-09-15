/*
 *  @author Philip Stutz
 *  @author Francisco de Freitas
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

package com.signalcollect.configuration

import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import com.signalcollect.factory.messagebus.AkkaMessageBusFactory
import akka.event.Logging.LogLevel
import akka.event.Logging
import com.signalcollect.factory.worker.AkkaWorkerFactory
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.factory.scheduler.Throughput
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.factory.mapper.DefaultMapperFactory
import com.signalcollect.storage.VertexMapStorage
import com.signalcollect.factory.storage.MemoryEfficientStorage
import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.signalcollect.messaging.BulkMessageBus
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import scala.reflect.ClassTag
import com.signalcollect.interfaces.ExistingVertexHandlerFactory
import com.signalcollect.interfaces.UndeliverableSignalHandlerFactory
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory

/**
 * All the graph configuration parameters with their defaults.
 */
case class GraphConfiguration[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag](
  actorSystem: Option[ActorSystem],
  actorNamePrefix: String,
  eagerIdleDetection: Boolean,
  consoleEnabled: Boolean,
  throttlingEnabled: Boolean,
  throttlingDuringLoadingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  consoleHttpPort: Int,
  loggingLevel: LogLevel,
  mapperFactory: MapperFactory[Id],
  storageFactory: StorageFactory[Id, Signal],
  schedulerFactory: SchedulerFactory[Id, Signal],
  preallocatedNodes: Option[Array[ActorRef]],
  nodeProvisioner: NodeProvisioner[Id, Signal],
  heartbeatIntervalInMilliseconds: Int,
  kryoRegistrations: List[String],
  kryoInitializer: String,
  serializeMessages: Boolean,
  workerFactory: WorkerFactory[Id, Signal],
  messageBusFactory: MessageBusFactory[Id, Signal],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Id, Signal],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Id, Signal],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal])
