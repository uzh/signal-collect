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

import signalcollect.implementations.coordinator.AsynchronousCoordinator
import signalcollect.interfaces._
import signalcollect.interfaces.ComputeGraph._
import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Worker._
import signalcollect.interfaces.MessageBus._
import signalcollect.interfaces.MessageRecipient

/**
 * [[signalcollect.interfaces.ComputeGraph]] implementation that executes
 * computations asynchronously.
 *
 * @param numberOfWorkers number of [[signalcollect.interfaces.Worker]]s
 * @param workerFactory factory to create worker instances
 * @param messageInboxFactory factory to create message queue instances
 * @param messageBusFactory factory used to create message bus instances
 * @param logger optional: used to process logging messages
 *
 * Signal/Collect supports two execution modes: synchronous and asynchronous.
 * These modes are about the order in which the signal/collect operations
 * on vertices are scheduled. A synchronous execution guarantees that all
 * vertices execute the same operation at the same time. In a synchronous
 * execution it can never happen that one vertex executes a signal operation
 * while another vertex is executing a collect operation, because the switch
 * from one phase to the other is globally synchronized. In contrast, the
 * asynchronous mode does not guarantee this. Consequently, the asynchronous
 * mode allows for optimizations by means of scheduling strategies and
 * operation scoring in the [[signalcollect.interfaces.Worker]]
 * implementation.
 */
class AsynchronousComputeGraph(
  numberOfWorkers: Int = ComputeGraph.defaultNumberOfThreads,
  workerFactory: WorkerFactory = Worker.defaultFactory,
  messageInboxFactory: QueueFactory = Queue.defaultFactory,
  messageBusFactory: MessageBusFactory = MessageBus.defaultFactory,
  logger: Option[MessageRecipient[Any]] = None)
  extends AsynchronousCoordinator(
    numberOfWorkers,
    workerFactory,
    messageInboxFactory,
    messageBusFactory,
    logger) {
  /** Java: no-arg constructor that uses default parameters */
  def this() = this(AsynchronousComputeGraph.init$default$1)
}