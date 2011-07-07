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
import signalcollect.configuration.provisioning._
import signalcollect.configuration.bootstrap._

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
class ComputeGraphBuilder(protected val config: Configuration = new DefaultConfiguration) extends Serializable {

  def build: ComputeGraph = {
    config.bootstrapConfiguration.executionArchitecture match {
      case LocalExecutionArchitecture => new LocalBootstrap(config).boot
      case DistributedExecutionArchitecture => new DistributedBootstrap(config).boot
    }
  }

  def withNumberOfWorkers(newNumberOfWorkers: Int) = newBuilder(numberOfWorkers = newNumberOfWorkers)
  def withLogger(logger: Boolean) = newBuilder(optionalLogger = logger)

  /**
   * Graph configuration
   */
  def withExecutionMode(newExecutionMode: ExecutionMode) = newBuilder(executionMode = newExecutionMode)
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newBuilder(messageBusFactory = newMessageBusFactory)
  def withStorageFactory(newStorageFactory: StorageFactory) = newBuilder(storageFactory = newStorageFactory)

  /**
   * Bootstrap configuration
   */
  def withExecutionArchitecture(newExecutionArchitecture: ExecutionArchitecture) = newBuilder(executionArchitecture = newExecutionArchitecture)
  def withNumberOfNodes(newNumberOfNodes: Int) = newBuilder(numberOfNodes = newNumberOfNodes)
  def withNodesAddress(newNodesAddress: Vector[String]) = newBuilder(nodesAddress = newNodesAddress)
  def withCoordinatorAddress(newCoordinatorAddress: String) = newBuilder(coordinatorAddress = newCoordinatorAddress)
  def withNodeProvisioning(newNodeProvisioning: NodeProvisioning) = newBuilder(nodeProvisioning = newNodeProvisioning)

  def newBuilder(
    numberOfWorkers: Int = config.numberOfWorkers,
    optionalLogger: Boolean = config.optionalLogger,
    // graph
    executionMode: ExecutionMode = config.executionConfiguration.executionMode,
    messageBusFactory: MessageBusFactory = config.graphConfiguration.messageBusFactory,
    storageFactory: StorageFactory = config.graphConfiguration.storageFactory,
    // bootstrap
    executionArchitecture: ExecutionArchitecture = config.bootstrapConfiguration.executionArchitecture,
    numberOfNodes: Int = config.bootstrapConfiguration.numberOfNodes,
    nodesAddress: Vector[String] = config.bootstrapConfiguration.nodesAddress,
    coordinatorAddress: String = config.bootstrapConfiguration.coordinatorAddress,
    nodeProvisioning: NodeProvisioning = config.bootstrapConfiguration.nodeProvisioning): ComputeGraphBuilder = {
    new ComputeGraphBuilder(
      DefaultConfiguration(
        numberOfWorkers = numberOfWorkers,
        optionalLogger = optionalLogger,
        graphConfiguration = DefaultGraphConfiguration(
          messageBusFactory = messageBusFactory,
          storageFactory = storageFactory),
        bootstrapConfiguration = DefaultBootstrapConfiguration(
          executionArchitecture = executionArchitecture,
          numberOfNodes = numberOfNodes,
          nodesAddress = nodesAddress,
          coordinatorAddress = coordinatorAddress,
          nodeProvisioning = nodeProvisioning),
        executionConfiguration = ExecutionConfiguration(executionMode = executionMode)))
  }

}
