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

package signalcollect.configuration

import signalcollect.api._
import signalcollect.interfaces._

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

  def build: ComputeGraph = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => new LocalBootstrap(config).boot
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }

  /**
   * Common configuration
   */
  def withNumberOfWorkers(newNumberOfWorkers: Int) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(numberOfWorkers = newNumberOfWorkers)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }
  def withLogger(logger: MessageRecipient[LogMessage]) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(customLogger = Some(logger))
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }
  def withExecutionArchitecture(newExecutionArchitecture: ExecutionArchitecture) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(executionArchitecture = newExecutionArchitecture)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }
  def withExecutionConfiguration(newExecutionConfiguration: ExecutionConfiguration) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(executionConfiguration = newExecutionConfiguration)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }

  /**
   * Worker configuration
   */
  def withWorkerFactory(newWorkerFactory: WorkerFactory) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(workerFactory = newWorkerFactory)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(messageBusFactory = newMessageBusFactory)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }
  def withStorageFactory(newStorageFactory: StorageFactory) = {
    config.executionArchitecture match {
      case LocalExecutionArchitecture => newLocalBuilder(storageFactory = newStorageFactory)
      case DistributedExecutionArchitecture => throw new Exception("Wrong Compute Graph Builder. Please use DistributedComputeGraphBuilder for distributed runs")
    }
  }

  /**
   * Builds local compute graph
   */
  def newLocalBuilder(
    numberOfWorkers: Int = config.numberOfWorkers,
    customLogger: Option[MessageRecipient[LogMessage]] = config.customLogger,
    workerFactory: WorkerFactory = config.workerConfiguration.workerFactory,
    messageBusFactory: MessageBusFactory = config.workerConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.workerConfiguration.storageFactory,
    executionArchitecture: ExecutionArchitecture = config.executionArchitecture,
    executionConfiguration: ExecutionConfiguration = config.executionConfiguration): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      DefaultLocalConfiguration(
        numberOfWorkers = numberOfWorkers,
        customLogger = customLogger,
        workerConfiguration = DefaultLocalWorkerConfiguration(
          workerFactory = workerFactory,
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        executionConfiguration = executionConfiguration))
  }

}
