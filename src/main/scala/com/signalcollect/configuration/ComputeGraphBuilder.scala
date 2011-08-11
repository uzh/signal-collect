/*
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

import com.signalcollect.api._
import com.signalcollect.interfaces._

/**
 * The configuration builder are intended for Java users.
 * These builders make configuring a compute graph with Java almost as simple
 * as when using Scala default parameters.
 */
object DefaultComputeGraphBuilder extends ComputeGraphBuilder

/**
 * Builder for the creation of a compute graph needs a configuration object for the creation.
 * If the user passes a configuration object but then uses a method of this class, the configuration's object
 * parameter gets overriden ("inserted" in the config object) by the method call's parameter which was passed.
 */
class ComputeGraphBuilder(protected val config: Configuration = new DefaultLocalConfiguration) extends Serializable {

  def build: ComputeGraph = new LocalBootstrap(config).boot

  /**
   * Common configuration
   */
  def withNumberOfWorkers(newNumberOfWorkers: Int) = newLocalBuilder(numberOfWorkers = newNumberOfWorkers)

  def withLoggingLevel(newLoggingLevel: Int) = newLocalBuilder(loggingLevel = newLoggingLevel)
  
  def withLogger(logger: MessageRecipient[LogMessage]) = newLocalBuilder(customLogger = Some(logger))

  def withExecutionConfiguration(newExecutionConfiguration: ExecutionConfiguration) = newLocalBuilder(executionConfiguration = newExecutionConfiguration)

  /**
   * Worker configuration
   */
  def withWorkerFactory(newWorkerFactory: WorkerFactory) = newLocalBuilder(workerFactory = newWorkerFactory)

  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newLocalBuilder(messageBusFactory = newMessageBusFactory)

  def withStorageFactory(newStorageFactory: StorageFactory) = newLocalBuilder(storageFactory = newStorageFactory)

  /**
   * Builds local compute graph
   */
  def newLocalBuilder(
    numberOfWorkers: Int = config.numberOfWorkers,
    loggingLevel: Int = LoggingLevel.Warning,
    customLogger: Option[MessageRecipient[LogMessage]] = config.customLogger,
    workerFactory: WorkerFactory = config.workerConfiguration.workerFactory,
    messageBusFactory: MessageBusFactory = config.workerConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.workerConfiguration.storageFactory,
    executionConfiguration: ExecutionConfiguration = config.executionConfiguration): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      DefaultLocalConfiguration(
        numberOfWorkers = numberOfWorkers,
        loggingLevel = loggingLevel,
        customLogger = customLogger,
        workerConfiguration = DefaultLocalWorkerConfiguration(
          workerFactory = workerFactory,
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        executionConfiguration = executionConfiguration))
  }

}
