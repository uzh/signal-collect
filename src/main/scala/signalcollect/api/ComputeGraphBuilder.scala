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

package signalcollect.api

import signalcollect.api.Factory._
import signalcollect.interfaces._

/**
 * The compute graph builders are intended for Java users.
 * These builders make configuring a compute graph with Java almost as simple
 * as when using Scala default parameters.
 */
object DefaultBuilder extends ComputeGraphBuilder
object DefaultSynchronousBuilder extends ComputeGraphBuilder(
  executionMode = SynchronousExecutionMode,
  workerFactory = Factory.Worker.Synchronous)

sealed trait ExecutionMode
object SynchronousExecutionMode extends ExecutionMode
object AsynchronousExecutionMode extends ExecutionMode

class ComputeGraphBuilder(
  executionMode: ExecutionMode = AsynchronousExecutionMode,
  numberOfWorkers: Int = ComputeGraph.defaultNumberOfThreadsUsed,
  workerFactory: WorkerFactory = Factory.Worker.Default,
  messageBusFactory: MessageBusFactory = Factory.MessageBus.Default,
  storageFactory: StorageFactory = Factory.Storage.Default,
  optionalLogger: Option[MessageRecipient[Any]] = None,
  signalThreshold: Double = 0.01,
  collectThreshold: Double = 0) {

  def build: ComputeGraph = new DefaultComputeGraph(
    executionMode,
    numberOfWorkers,
    workerFactory,
    messageBusFactory,
    storageFactory,
    optionalLogger,
    signalThreshold,
    collectThreshold)

  def withExecutionMode(newExecutionMode: ExecutionMode) = newComputeGraph(newExecutionMode = newExecutionMode)
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newComputeGraph(newNumberOfWorkers = newNumberOfWorkers)
  def withWorkerFactory(newWorkerFactory: WorkerFactory) = newComputeGraph(newWorkerFactory = newWorkerFactory)
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newComputeGraph(newMessageBusFactory = newMessageBusFactory)
  def withStorageFactory(newStorageFactory: StorageFactory) = newComputeGraph(newStorageFactory = newStorageFactory)
  def withLogger(logger: MessageRecipient[Any]) = newComputeGraph(newOptionalLogger = Some(logger))
  def withSignalThreshold(newSignalThreshold: Double) = newComputeGraph(newSignalThreshold = newSignalThreshold)
  def withCollectThreshold(newCollectThreshold: Double) = newComputeGraph(newCollectThreshold = newCollectThreshold)

  def newComputeGraph(
    newExecutionMode: ExecutionMode = executionMode,
    newNumberOfWorkers: Int = numberOfWorkers,
    newWorkerFactory: WorkerFactory = workerFactory,
    newMessageBusFactory: MessageBusFactory = messageBusFactory,
    newStorageFactory: StorageFactory = storageFactory,
    newOptionalLogger: Option[MessageRecipient[Any]] = optionalLogger,
    newSignalThreshold: Double = 0.01,
    newCollectThreshold: Double = 0): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      newExecutionMode,
      newNumberOfWorkers,
      newWorkerFactory,
      newMessageBusFactory,
      newStorageFactory,
      newOptionalLogger,
      newSignalThreshold,
      newCollectThreshold)
  }
}
