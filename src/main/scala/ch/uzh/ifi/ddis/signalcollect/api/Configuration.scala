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

package ch.uzh.ifi.ddis.signalcollect.api

import ch.uzh.ifi.ddis.signalcollect.implementations.logging.DefaultLogger
import ch.uzh.ifi.ddis.signalcollect.interfaces.MessageRecipient
import ch.uzh.ifi.ddis.signalcollect.implementations.messaging.Verbosity
import ch.uzh.ifi.ddis.signalcollect.interfaces.MessageBus
import ch.uzh.ifi.ddis.signalcollect._
import ch.uzh.ifi.ddis.signalcollect.implementations.messaging.MultiQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.forkjoin.LinkedTransferQueue
import ch.uzh.ifi.ddis.signalcollect.implementations.worker.AsynchronousPriorityWorker
import ch.uzh.ifi.ddis.signalcollect.api.Queues._
import ch.uzh.ifi.ddis.signalcollect.implementations.worker.AsynchronousWorker
import ch.uzh.ifi.ddis.signalcollect.implementations.worker.DirectDeliveryAsynchronousWorker
import ch.uzh.ifi.ddis.signalcollect.implementations.worker.SynchronousWorker
import ch.uzh.ifi.ddis.signalcollect.interfaces.Worker

object Workers {
	type WorkerFactory = (interfaces.MessageBus[Any, Any], QueueFactory) => Worker
	
	lazy val defaultFactory = asynchronousDirectDeliveryWorkerFactory
	def createSynchronousWorker(mb: interfaces.MessageBus[Any, Any], qf: QueueFactory) = new SynchronousWorker(mb, qf)
	lazy val synchronousWorkerFactory = createSynchronousWorker _
	def createAsynchronousWorker(mb: interfaces.MessageBus[Any, Any], qf: QueueFactory) = new AsynchronousWorker(mb, qf)
	lazy val asynchronousWorkerFactory = createAsynchronousWorker _
	def createAsynchronousDirectDeliveryWorker(mb: interfaces.MessageBus[Any, Any], qf: QueueFactory) = new DirectDeliveryAsynchronousWorker(mb, qf)
	lazy val asynchronousDirectDeliveryWorkerFactory = createAsynchronousDirectDeliveryWorker _
	def createAsynchronousPriorityWorker(mb: interfaces.MessageBus[Any, Any], qf: QueueFactory) = new AsynchronousPriorityWorker(mb, qf)
	lazy val asynchronousPriorityWorkerFactory = createAsynchronousPriorityWorker _
}

object Queues {
	type QueueFactory = () => BlockingQueue[Any]
	lazy val defaultFactory = linkedTransferQueueFactory
	
	def createLinkedTransferQueue = new LinkedTransferQueue[Any]
	lazy val linkedTransferQueueFactory = createLinkedTransferQueue _
	
	def createLinkedBlockingQueue = new LinkedBlockingQueue[Any]
	lazy val linkedBlockingQueueFactory = createLinkedBlockingQueue _
	
	def createMultiQueue(qf: QueueFactory = linkedBlockingQueueFactory, numberOfQueues: Int = ComputeGraph.defaultNumberOfThreads) = new MultiQueue[Any](qf, numberOfQueues)
	lazy val multiQueueFactory = createMultiQueue _
}

object MessageBuses {
	type MessageBusFactory = () => MessageBus[Any, Any]
	lazy val defaultFactory = sharedMemoryMessageBusFactory
	
	def createSharedMemoryMessageBus: MessageBus[Any, Any] = new implementations.messaging.DefaultMessageBus[Any, Any]
	lazy val sharedMemoryMessageBusFactory = createSharedMemoryMessageBus _
	
	def createVerboseSharedMemoryMessageBus: MessageBus[Any, Any] = new implementations.messaging.DefaultMessageBus[Any, Any] with Verbosity[Any, Any]
	lazy val verboseMessageBusFactory = createVerboseSharedMemoryMessageBus _
}

object Loggers {
	def createDefault = createConsoleLogger 
	
	def createConsoleLogger: MessageRecipient[Any] = new DefaultLogger
}