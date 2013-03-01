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

import com.signalcollect.interfaces.LogMessage
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import com.signalcollect.logging.DefaultLogger
import com.signalcollect.factory.worker.LocalWorker
import com.signalcollect.factory.messagebus.AkkaMessageBusFactory
import com.signalcollect.factory.storage.DefaultStorage

/**
 * All the graph configuration parameters with their defaults.
 */
case class GraphConfiguration(
  consoleEnabled: Boolean = false,
  consoleHttpPort: Int = -1,
  loggingLevel: Int = LoggingLevel.Warning,
  logger: LogMessage => Unit = DefaultLogger.log,
  workerFactory: WorkerFactory = LocalWorker,
  messageBusFactory: MessageBusFactory = AkkaMessageBusFactory,
  storageFactory: StorageFactory = DefaultStorage,
  statusUpdateIntervalInMilliseconds: Long = 500l,
  akkaDispatcher: AkkaDispatcher = Pinned,
  akkaMessageCompression: Boolean = false,
  nodeProvisioner: NodeProvisioner = new LocalNodeProvisioner,
  heartbeatIntervalInMilliseconds: Long = 100)

object LoggingLevel {
  val Debug = 0
  val Config = 100
  val Info = 200
  val Warning = 300
  val Severe = 400
}

sealed trait AkkaDispatcher
case object EventBased extends AkkaDispatcher
case object Pinned extends AkkaDispatcher
