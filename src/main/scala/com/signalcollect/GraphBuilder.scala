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

import scala.reflect.ClassTag

import com.signalcollect.configuration.AkkaDispatcher
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.nodeprovisioning.NodeProvisioner
import akka.event.Logging.LogLevel
import akka.event.Logging

/**
 *  A graph builder holds a configuration with parameters for building a graph,
 *  functions to modify this configuration and a build function to instantiate a graph
 *  with the defined configuration. This object represents a graph builder that is initialized with the default configuration.
 */
object GraphBuilder extends GraphBuilder[Any, Any](GraphConfiguration())

/**
 * Configurable builder for a Signal/Collect graph.
 *
 *  @author Philip Stutz
 */
class GraphBuilder[Id: ClassTag, Signal: ClassTag](protected val config: GraphConfiguration = GraphConfiguration()) extends Serializable {

  /**
   *  Creates a graph with the specified configuration.
   */
  def build: Graph[Id, Signal] = new DefaultGraph[Id, Signal](config)

  /**
   *  Configures if the console website on port 8080 is enabled.
   */
  def withConsole(newConsoleEnabled: Boolean) = newLocalBuilder(consoleEnabled = newConsoleEnabled)

  /**
   *  Configures if the console website on a configurable port is enabled.
   */
  def withConsole(newConsoleEnabled: Boolean, newConsoleHttpPort: Int) =
    newLocalBuilder(consoleEnabled = newConsoleEnabled, consoleHttpPort = newConsoleHttpPort)

  /**
   *  Configures if Akka message compression is enabled.
   */
  def withAkkaMessageCompression(newAkkaMessageCompression: Boolean) = newLocalBuilder(akkaMessageCompression = newAkkaMessageCompression)

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
  def withLoggingLevel(newLoggingLevel: LogLevel) = newLocalBuilder(loggingLevel = newLoggingLevel)

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
   *  Configures the status update interval (in milliseconds).
   */
  def withStatusUpdateInterval(newStatusUpdateInterval: Long) = newLocalBuilder(statusUpdateIntervalInMilliseconds = newStatusUpdateInterval)

  /**
   *  Configures the Akka dispatcher for the worker actors.
   */
  def withAkkaDispatcher(newAkkaDispatcher: AkkaDispatcher) = newLocalBuilder(akkaDispatcher = newAkkaDispatcher)

  /**
   *  Configures the node provider.
   *
   *  @param newNodeProvider The node provider will acquire the resources for running a graph algorithm.
   */
  def withNodeProvisioner(newNodeProvisioner: NodeProvisioner) = newLocalBuilder(nodeProvisioner = newNodeProvisioner)

  /**
   *  Configures the interval with which the coordinator sends a heartbeat to the workers.
   *
   *  @param newHeartbeatIntervalInMilliseconds The interval with which the coordinator sends a heartbeat to the workers.
   */
  def withHeartbeatInterval(newHeartbeatIntervalInMilliseconds: Int) = newLocalBuilder(heartbeatIntervalInMilliseconds = newHeartbeatIntervalInMilliseconds)

  /**
   *  Specifies additional Kryo serialization registrations.
   */
  def withKryoRegistrations(newKryoRegistrations: List[String]) = newLocalBuilder(kryoRegistrations = newKryoRegistrations)

  /**
   *  If true forces Akka message serialization even in local settings. For debugging purposes only.
   */
  def withMessageSerialization(newSerializeMessages: Boolean) = newLocalBuilder(serializeMessages = newSerializeMessages)

  /**
   *  Internal function to create a new builder instance that has a configuration which defaults
   *  to parameters that are the same as the ones in this instance, unless explicitly set differently.
   */
  protected def newLocalBuilder(
    consoleEnabled: Boolean = config.consoleEnabled,
    consoleHttpPort: Int = config.consoleHttpPort,
    loggingLevel: Logging.LogLevel = config.loggingLevel,
    workerFactory: WorkerFactory = config.workerFactory,
    messageBusFactory: MessageBusFactory = config.messageBusFactory,
    storageFactory: StorageFactory = config.storageFactory,
    statusUpdateIntervalInMilliseconds: Long = config.statusUpdateIntervalInMilliseconds,
    akkaDispatcher: AkkaDispatcher = config.akkaDispatcher,
    akkaMessageCompression: Boolean = config.akkaMessageCompression,
    nodeProvisioner: NodeProvisioner = config.nodeProvisioner,
    heartbeatIntervalInMilliseconds: Int = config.heartbeatIntervalInMilliseconds,
    kryoRegistrations: List[String] = config.kryoRegistrations,
    serializeMessages: Boolean = config.serializeMessages): GraphBuilder[Id, Signal] = {
    new GraphBuilder[Id, Signal](
      GraphConfiguration(
        consoleEnabled = consoleEnabled,
        consoleHttpPort = consoleHttpPort,
        loggingLevel = loggingLevel,
        workerFactory = workerFactory,
        messageBusFactory = messageBusFactory,
        storageFactory = storageFactory,
        statusUpdateIntervalInMilliseconds = statusUpdateIntervalInMilliseconds,
        akkaDispatcher = akkaDispatcher,
        akkaMessageCompression = akkaMessageCompression,
        nodeProvisioner = nodeProvisioner,
        heartbeatIntervalInMilliseconds = heartbeatIntervalInMilliseconds,
        kryoRegistrations = kryoRegistrations,
        serializeMessages = serializeMessages))
  }

}
