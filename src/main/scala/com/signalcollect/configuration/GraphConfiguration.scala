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

import com.signalcollect.interfaces._
import java.util.HashMap
import com.signalcollect._
import com.signalcollect.logging.DefaultLogger
import akka.actor.ActorRef
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner

/**
 * All the graph configuration parameters with their defaults.
 */
case class GraphConfiguration(
  maxInboxSize: Option[Long] = Some(Runtime.getRuntime.availableProcessors * 5000), //None
  loggingLevel: Int = LoggingLevel.Warning,
  logger: LogMessage => Unit = DefaultLogger.log,
  workerFactory: WorkerFactory = factory.worker.Akka,
  messageBusFactory: MessageBusFactory = factory.messagebus.SharedMemory,
  storageFactory: StorageFactory = factory.storage.InMemory,
  statusUpdateIntervalInMillis: Option[Long] = Some(500l),
  akkaDispatcher: AkkaDispatcher = Pinned,
  nodeProvisioner: NodeProvisioner = new LocalNodeProvisioner)

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
