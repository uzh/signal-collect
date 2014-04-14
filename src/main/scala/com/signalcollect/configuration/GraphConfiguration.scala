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
import com.signalcollect.factory.worker.DefaultAkkaWorker
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.factory.scheduler.Throughput
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.factory.mapper.DefaultMapperFactory
import com.signalcollect.storage.VertexMapStorage
import com.signalcollect.factory.storage.MemoryEfficientStorage
import akka.actor.ActorRef

/**
 * All the graph configuration parameters with their defaults.
 */
case class GraphConfiguration(
  consoleEnabled: Boolean = false,
  consoleHttpPort: Int = -1,
  loggingLevel: LogLevel = Logging.WarningLevel,
  workerFactory: WorkerFactory = DefaultAkkaWorker,
  messageBusFactory: MessageBusFactory = AkkaMessageBusFactory,
  mapperFactory: MapperFactory = DefaultMapperFactory,
  storageFactory: StorageFactory = MemoryEfficientStorage,
  schedulerFactory: SchedulerFactory = Throughput,
  statusUpdateIntervalInMilliseconds: Long = 500l,
  akkaDispatcher: AkkaDispatcher = Pinned,
  akkaMessageCompression: Boolean = false,
  preallocatedNodes: Option[Array[ActorRef]] = None,
  nodeProvisioner: NodeProvisioner = new LocalNodeProvisioner(),
  heartbeatIntervalInMilliseconds: Int = 100,
  kryoRegistrations: List[String] = List(),
  serializeMessages: Boolean = false,
  useJavaSerialization: Boolean = true)

sealed trait AkkaDispatcher
object EventBased extends AkkaDispatcher
object Pinned extends AkkaDispatcher
