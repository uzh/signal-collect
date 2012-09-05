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

package com.signalcollect

import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.nodeprovisioning.NodeProvisioner

/**
 *  A graph builder holds a configuration with parameters for building a graph,
 *  functions to modify this configuration and a build function to instantiate a graph
 *  with the defined configuration. This object represents a graph builder that is initialized with the default configuration.
 */
object GraphBuilder extends GraphBuilder(GraphConfiguration())

/**
 * Builder for the creation of a compute graph needs a configuration object for the creation.
 * If the user passes a configuration object but then uses a method of this class, the configuration's object
 * parameter gets overriden ("inserted" in the config object) by the method call's parameter which was passed.
 *
 *  @author Philip Stutz
 */
class GraphBuilder(protected val config: GraphConfiguration = GraphConfiguration()) extends Serializable {

  /**
   *  Creates a graph with the specified configuration.
   */
  def build: Graph = new DefaultGraph(config)

  /**
   *  Configures the node provider.
   *
   *  @param newNodeProvider The node provider will acquire the resources for running a graph algorithm.
   */
  def withNodeProvisioner(newNodeProvisioner: NodeProvisioner) = newLocalBuilder(nodeProvisioner = newNodeProvisioner)

  /**
   *  Configures the logging level.
   *
   *  @note Logging levels available:
   *    Debug = 0
   *    Config = 100
   *    Info = 200
   *    Warning = 300
   *    Severe = 400
   *
   *  @param newLoggingLevel The logging level used by the graph.
   */
  def withLoggingLevel(newLoggingLevel: Int) = newLocalBuilder(loggingLevel = newLoggingLevel)

  /**
   *  Configures the logger used by the graph.
   *
   *  @param logger The logger used by the graph.
   */
  def withLogger(logger: LogMessage => Unit) = newLocalBuilder(logger = logger)

  /**
   *  Configures the worker factory used by the graph to instantiate workers.
   *
   *  @param newWorkerFactory The worker factory used to instantiate workers.
   */
  def withWorkerFactory(newWorkerFactory: WorkerFactory) = newLocalBuilder(workerFactory = newWorkerFactory)

  /**
   *  Configures the message bus factory used by the graph to instantiate message buses.
   *
   *  @param newMessageBusFactory The message bus factory used to instantiate message buses.
   */
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory) = newLocalBuilder(messageBusFactory = newMessageBusFactory)

  /**
   *  Configures the storage factory used by the workers to instantiate vertex stores.
   *
   *  @param newStorageFactory The storage factory used to instantiate vertex stores.
   */
  def withStorageFactory(newStorageFactory: StorageFactory) = newLocalBuilder(storageFactory = newStorageFactory)

  /**
   *  Internal function to create a new builder instance that has a configuration which defaults
   *  to parameters that are the same as the ones in this instance, unless explicitly set differently.
   */
  protected def newLocalBuilder(
    loggingLevel: Int = config.loggingLevel,
    logger: LogMessage => Unit = config.logger,
    workerFactory: WorkerFactory = config.workerFactory,
    messageBusFactory: MessageBusFactory = config.messageBusFactory,
    storageFactory: StorageFactory = config.storageFactory,
    nodeProvisioner: NodeProvisioner = config.nodeProvisioner): GraphBuilder = {
    new GraphBuilder(
      GraphConfiguration(
        maxInboxSize = config.maxInboxSize, 
        loggingLevel = loggingLevel,
        logger = logger,
        workerFactory = workerFactory,
        messageBusFactory = messageBusFactory,
        storageFactory = storageFactory,
        statusUpdateIntervalInMillis = config.statusUpdateIntervalInMillis,
        akkaDispatcher = config.akkaDispatcher,
        nodeProvisioner = nodeProvisioner))
  }

}
