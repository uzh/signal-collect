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

import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.factory.handler.DefaultEdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.factory.handler.DefaultExistingVertexHandlerFactory
import com.signalcollect.factory.handler.DefaultUndeliverableSignalHandlerFactory
import com.signalcollect.factory.mapper.DefaultMapperFactory
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import com.signalcollect.factory.scheduler.Throughput
import com.signalcollect.factory.storage.MemoryEfficientStorage
import com.signalcollect.factory.worker.AkkaWorkerFactory
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.interfaces.ExistingVertexHandlerFactory
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.UndeliverableSignalHandlerFactory
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel

/**
 *  A graph builder holds a configuration with parameters for building a graph,
 *  functions to modify this configuration and a build function to instantiate a graph
 *  with the defined configuration. This object represents a graph builder that is initialized with the default configuration.
 */
object GraphBuilder extends GraphBuilder[Any, Any](None)

/**
 * Configurable builder for a Signal/Collect graph.
 *
 *  @author Philip Stutz
 */
class GraphBuilder[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag](
  configOption: Option[GraphConfiguration[Id, Signal]] = None) extends Serializable {

  val config = configOption.getOrElse(
    new GraphConfiguration[Id, Signal](
      actorSystem = None,
      actorNamePrefix = "",
      eagerIdleDetection = true,
      consoleEnabled = false,
      throttlingEnabled = false,
      throttlingDuringLoadingEnabled = false,
      supportBlockingGraphModificationsInVertex = true,
      consoleHttpPort = -1,
      loggingLevel = Logging.WarningLevel,
      mapperFactory = new DefaultMapperFactory[Id],
      storageFactory = new MemoryEfficientStorage[Id, Signal],
      schedulerFactory = new Throughput[Id, Signal],
      preallocatedNodes = None,
      nodeProvisioner = new LocalNodeProvisioner[Id, Signal](),
      heartbeatIntervalInMilliseconds = 100,
      kryoRegistrations = List(),
      kryoInitializer = "com.signalcollect.configuration.KryoInit",
      serializeMessages = false,
      workerFactory = new AkkaWorkerFactory[Id, Signal],
      messageBusFactory = new BulkAkkaMessageBusFactory[Id, Signal](1000, true),
      existingVertexHandlerFactory = new DefaultExistingVertexHandlerFactory[Id, Signal](),
      undeliverableSignalHandlerFactory = new DefaultUndeliverableSignalHandlerFactory[Id, Signal],
      edgeAddedToNonExistentVertexHandlerFactory = new DefaultEdgeAddedToNonExistentVertexHandlerFactory[Id, Signal]))

  /**
   *  Creates a graph with the specified configuration.
   */
  def build: Graph[Id, Signal] = new DefaultGraph[Id, Signal](config)

  protected def builder(config: GraphConfiguration[Id, Signal]) =
    new GraphBuilder[Id, Signal](Some(config))

  /**
   *  When support is enabled, workers use a special message bus that prevents deadlocks.
   */
  def withBlockingGraphModificationsSupport(supported: Boolean) = {
    builder(config.copy(supportBlockingGraphModificationsInVertex = supported))
  }

  /**
   *  Configures the factory that is used to create the handlers for situations
   *  in which a new vertex is added when a vertex with the same ID already exists.
   *  The default handler silently discards the redundant new vertex.
   */
  def withExistingVertexHandlerFactory(newExistingVertexHandlerFactory: ExistingVertexHandlerFactory[Id, Signal]) = {
    builder(config.copy(existingVertexHandlerFactory = newExistingVertexHandlerFactory))
  }

  /**
   *  Configures the factory that is used to create the handlers for situations
   *  in which the recipient vertex of a signal does not exist.
   *  The default handler throws an exception.
   */
  def withUndeliverableSignalHandlerFactory(newUndeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Id, Signal]) = {
    builder(config.copy(undeliverableSignalHandlerFactory = newUndeliverableSignalHandlerFactory))
  }

  /**
   *  Configures the factory that is used to create the handlers for situations
   *  in which the an edge is added to a vertex that does not exist (yet).
   *  The default handler throws an exception.
   */
  def withEdgeAddedToNonExistentVertexHandlerFactory(newEdgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal]) = {
    builder(config.copy(edgeAddedToNonExistentVertexHandlerFactory = newEdgeAddedToNonExistentVertexHandlerFactory))
  }

  /**
   *  Configures if workers should eagerly notify the node about their idle state.
   *  This speeds up idle detection and with it the latency between computation steps,
   *  but at the cost of an increased messaging overhead.
   */
  def withEagerIdleDetection(newEagerIdleDetection: Boolean) = {
    builder(config.copy(eagerIdleDetection = newEagerIdleDetection))
  }

  /**
   *  When throttling is enabled, the workers monitor the
   *  messaging load of the system and stop signaling in case
   *  the system should get overloaded.
   *  @note: Throttling during graph loading is enabled with a separate flag.
   */
  def withThrottlingEnabled(newThrottlingEnabled: Boolean) = {
    builder(config.copy(throttlingEnabled = newThrottlingEnabled))
  }

  /**
   *  Sets if throttling should be active during graph loading with Graph.loadGraph.
   */
  def withThrottlingDuringLoadingEnabled(newThrottlingDuringLoadingEnabled: Boolean) = {
    builder(config.copy(throttlingDuringLoadingEnabled = newThrottlingDuringLoadingEnabled))
  }

  /**
   *  Configures if the console website on port 8080 is enabled.
   */
  def withConsole(newConsoleEnabled: Boolean) =
    builder(config.copy(consoleEnabled = newConsoleEnabled))

  /**
   *  Configures if the console website on a configurable port is enabled.
   */
  def withConsole(newConsoleEnabled: Boolean, newConsoleHttpPort: Int) =
    builder(config.copy(consoleEnabled = newConsoleEnabled, consoleHttpPort = newConsoleHttpPort))

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
  def withLoggingLevel(newLoggingLevel: LogLevel) =
    builder(config.copy(loggingLevel = newLoggingLevel))

  /**
   *  Configures the worker factory used by the graph to instantiate workers.
   *
   *  @param newWorkerFactory The worker factory used to instantiate workers.
   */
  def withWorkerFactory(newWorkerFactory: WorkerFactory[Id, Signal]) =
    builder(config.copy(workerFactory = newWorkerFactory))

  /**
   *  Configures the message bus factory used by the graph to instantiate message buses.
   *
   *  @param newMessageBusFactory The message bus factory used to instantiate message buses.
   */
  def withMessageBusFactory(newMessageBusFactory: MessageBusFactory[Id, Signal]) =
    builder(config.copy(messageBusFactory = newMessageBusFactory))

  /**
   *  Configures the mapper factory used by the graph to instantiate mappers that assign vertices to workers.
   *
   *  @param newMapperFactory The mapper factory used to instantiate vertex to worker mappers.
   */
  def withMapperFactory(newMapperFactory: MapperFactory[Id]) =
    builder(config.copy(mapperFactory = newMapperFactory))

  /**
   *  Configures the storage factory used by the workers to instantiate vertex stores.
   *
   *  @param newStorageFactory The storage factory used to instantiate vertex stores.
   */
  def withStorageFactory(newStorageFactory: StorageFactory[Id, Signal]) =
    builder(config.copy(storageFactory = newStorageFactory))

  /**
   *  Configures the scheduler factory used by the workers to instantiate schedulers.
   *
   *  @param newSchedulerFactory The scheduler factory used to instantiate schedulers.
   */
  def withSchedulerFactory(newSchedulerFactory: SchedulerFactory[Id, Signal]) =
    builder(config.copy(schedulerFactory = newSchedulerFactory))

  /**
   *  Initializes S/C with preallocated node actors.
   *  When using preallocated nodes, then the node provisioner will not be used.
   *
   *  @param nodes The nodes on which Signal/Collect will deploy the workers.
   */
  def withPreallocatedNodes(nodes: Array[ActorRef]) =
    builder(config.copy(preallocatedNodes = Some(nodes)))

  /**
   *  Initializes S/C with an already existing actor system.
   *
   *  Using this option will mean that the user is responsible for
   *  configuring options related to serialization, networking,
   *  port, etc in a way that agrees with S/C.
   *
   *  @note: The logging inside the default vertices and edges only works when the
   *  actor system is called "SignalCollect".
   *
   *  @param system: The actor system on which S/C will be deployed.
   */
  def withActorSystem(system: ActorSystem) =
    builder(config.copy(actorSystem = Some(system)))

  /**
   * Instructs S/C to use a prefix for all actor names.
   * This allows multiple instances of S/C to run on the
   * same actor system, as long as they use different prefixes.
   *
   * @param prefix: The prefix that S/C uses for actor names.
   */
  def withActorNamePrefix(prefix: String) =
    builder(config.copy(actorNamePrefix = prefix))

  /**
   *  Configures the node provider.
   *
   *  @param newNodeProvider The node provider will acquire the resources for running a graph algorithm.
   */
  def withNodeProvisioner(newNodeProvisioner: NodeProvisioner[Id, Signal]) =
    builder(config.copy(nodeProvisioner = newNodeProvisioner))

  /**
   *  Configures the interval with which the coordinator sends a heartbeat to the workers.
   *
   *  @param newHeartbeatIntervalInMilliseconds The interval with which the coordinator sends a heartbeat to the workers.
   */
  def withHeartbeatInterval(newHeartbeatIntervalInMilliseconds: Int) =
    builder(config.copy(heartbeatIntervalInMilliseconds = newHeartbeatIntervalInMilliseconds))

  /**
   *  Specifies additional Kryo serialization registrations.
   */
  def withKryoRegistrations(newKryoRegistrations: List[String]) =
    builder(config.copy(kryoRegistrations = newKryoRegistrations))

  /**
   *  If true forces Akka message serialization even in local settings.
   */
  def withMessageSerialization(newSerializeMessages: Boolean) =
    builder(config.copy(serializeMessages = newSerializeMessages))

  /**
   *  Sets the fully qualified class name of the Kryo initializer. This class can be used to
   *  set advanced Kryo configuration parameters.
   *  Additional documentation can be found at https://github.com/romix/akka-kryo-serialization,
   *  in the section "How to create a custom initializer for Kryo".
   */
  def withKryoInitializer(newKryoInitializer: String) =
    builder(config.copy(kryoInitializer = newKryoInitializer))

}
