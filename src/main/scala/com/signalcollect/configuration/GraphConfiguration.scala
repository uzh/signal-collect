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

package com.signalcollect.configuration

import com.signalcollect.interfaces._
import java.util.HashMap
import com.signalcollect._
import com.signalcollect.implementations.logging.DefaultLogger

/**
 * All the graph configuration parameters with their defaults.
 */
case class GraphConfiguration(
  numberOfWorkers: Int = Runtime.getRuntime.availableProcessors,
  maxInboxSize: Option[Long] = Some(Runtime.getRuntime.availableProcessors * 5000), //None
  loggingLevel: Int = LoggingLevel.Warning,
  logger: MessageRecipient[LogMessage] = new DefaultLogger,
  workerConfiguration: WorkerConfiguration = WorkerConfiguration())

/**
 * All the worker configuration parameters with their defaults.
 */
case class WorkerConfiguration(
  workerFactory: WorkerFactory = factory.worker.Local,
  messageBusFactory: MessageBusFactory = factory.messagebus.SharedMemory,
  storageFactory: StorageFactory = factory.storage.InMemory,
  statusUpdateIntervalInMillis: Option[Long] = None)
