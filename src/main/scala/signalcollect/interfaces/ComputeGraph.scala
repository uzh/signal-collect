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

package signalcollect.interfaces

import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Worker._
import signalcollect.interfaces.MessageBus._
import signalcollect.interfaces.Storage._

object ComputeGraph {
  lazy val defaultNumberOfThreads = Runtime.getRuntime.availableProcessors
}

trait ComputeGraph extends GraphApi {
  def execute: ComputationStatistics
  def shutDown

  def foreach(f: (Vertex[_, _]) => Unit)
  def foreach(f: PartialFunction[Vertex[_, _], Unit])

  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long
  //  def countEdges: Long

  def sum[N](implicit numeric: Numeric[N]): N
  def product[N](implicit numeric: Numeric[N]): N
  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType]
  def aggregateStates[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType): ValueType
  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType

  def setSignalThreshold(t: Double)
  def setCollectThreshold(t: Double)
  def setStepsLimit(l: Int)
}

object DefaultConfig extends ComputeGraphConfig
object DefaultSynchronousConfig extends ComputeGraphConfig(
  executionMode = SynchronousExecutionMode,
  workerFactory = Worker.synchronousDirectDeliveryWorkerFactory)

sealed trait ExecutionMode
object SynchronousExecutionMode extends ExecutionMode
object AsynchronousExecutionMode extends ExecutionMode

case class ComputeGraphConfig(
  executionMode: ExecutionMode = AsynchronousExecutionMode,
  numberOfWorkers: Int = ComputeGraph.defaultNumberOfThreads,
  workerFactory: WorkerFactory = Worker.defaultFactory,
  messageInboxFactory: QueueFactory = Queue.defaultFactory,
  messageBusFactory: MessageBusFactory = MessageBus.defaultFactory,
  storageFactory: StorageFactory = Storage.defaultFactory,
  optionalLogger: Option[MessageRecipient[Any]] = None) {

  def withExecutionMode(newExecutionMode: ExecutionMode) = newComputeGraph(newExecutionMode = newExecutionMode)
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newComputeGraph(newNumberOfWorkers = newNumberOfWorkers)
  def withWorkerFactory(newNumberOfWorkers: Int) = newComputeGraph(newNumberOfWorkers = newNumberOfWorkers)
  def withMessageInboxFactory(newMessageInboxFactory: QueueFactory) = newComputeGraph(newMessageInboxFactory = newMessageInboxFactory)
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newComputeGraph(newMessageBusFactory = newMessageBusFactory)
  def withStorageFactory(newStorageFactory: StorageFactory) = newComputeGraph(newStorageFactory = newStorageFactory)
  def withLogger(logger: MessageRecipient[Any]) = newComputeGraph(newOptionalLogger = Some(logger))

  def newComputeGraph(
    newExecutionMode: ExecutionMode = executionMode,
    newNumberOfWorkers: Int = numberOfWorkers,
    newWorkerFactory: WorkerFactory = workerFactory,
    newMessageInboxFactory: QueueFactory = messageInboxFactory,
    newMessageBusFactory: MessageBusFactory = messageBusFactory,
    newStorageFactory: StorageFactory = storageFactory,
    newOptionalLogger: Option[MessageRecipient[Any]] = optionalLogger): ComputeGraphConfig = {
    ComputeGraphConfig(
      newExecutionMode,
      newNumberOfWorkers,
      newWorkerFactory,
      newMessageInboxFactory,
      newMessageBusFactory,
      newStorageFactory,
      newOptionalLogger)
  }
}



    





