/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package signalcollect.api

import signalcollect.implementations.coordinator.SynchronousCoordinator
import signalcollect.interfaces._
import signalcollect.interfaces.ComputeGraph._
import signalcollect.interfaces.MessageRecipient
import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Worker._
import signalcollect.interfaces.MessageBus._
import signalcollect._

class SynchronousComputeGraph(
	  numberOfWorkers: Int = ComputeGraph.defaultNumberOfThreads,
	  workerFactory: WorkerFactory = Worker.synchronousDirectDeliveryWorkerFactory,
	  messageInboxFactory: QueueFactory = Queue.defaultFactory,
	  messageBusFactory: MessageBusFactory = MessageBus.defaultFactory,
	  logger: Option[MessageRecipient[Any]] = None
	) extends SynchronousCoordinator(
	  numberOfWorkers,
	  workerFactory,
	  messageInboxFactory,
	  messageBusFactory,
	  logger
  )