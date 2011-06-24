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

import signalcollect.interfaces._

/**
 * The compute graph builders are intended for Java users.
 * These builders make configuring a compute graph with Java almost as simple
 * as when using Scala default parameters.
 */
object DefaultBuilder extends ComputeGraphBuilder

class ComputeGraphBuilder(protected val config: Configuration = new DefaultConfiguration) extends Serializable {
  def build: ComputeGraph = new DefaultComputeGraph(config)

  def withExecutionMode(newExecutionMode: ExecutionMode) = newBuilder(executionMode = newExecutionMode)
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newBuilder(numberOfWorkers = newNumberOfWorkers)
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newBuilder(messageBusFactory = newMessageBusFactory)
  def withStorageFactory(newStorageFactory: StorageFactory) = newBuilder(storageFactory = newStorageFactory)
  def withLogger(logger: MessageRecipient[Any]) = newBuilder(optionalLogger = Some(logger))

  def newBuilder(
    executionMode: ExecutionMode = config.executionMode,
    numberOfWorkers: Int = config.numberOfWorkers,
    messageBusFactory: MessageBusFactory = config.messageBusFactory,
    storageFactory: StorageFactory = config.storageFactory,
    optionalLogger: Option[MessageRecipient[Any]] = config.optionalLogger): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      DefaultConfiguration(
        executionMode = executionMode,
        numberOfWorkers = numberOfWorkers,
        messageBusFactory = messageBusFactory,
        storageFactory = storageFactory,
        optionalLogger = optionalLogger))
  }
}
