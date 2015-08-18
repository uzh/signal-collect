package com.signalcollect.configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import akka.event.Logging._
import akka.event.Logging
import java.net.InetAddress
import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import net.ceedubs.ficus.Ficus._
import scala.remote
import collection.JavaConversions._
import com.typesafe.config.ConfigObject

object Akka {

  private[this] def value(v: Any): ConfigValue = {
    v match {
      case s: ConfigValue => s
      case m: Map[String @unchecked, _] => ConfigValueFactory.fromMap(m)
      case l: List[_]                   => ConfigValueFactory.fromIterable(l)
      case _ => ConfigValueFactory.fromAnyRef(v)
    }
  }

  val serializeMessagesPath = "akka.actor.serialize-messages"
  val serializeMessagesValues: Boolean => ConfigValue = {
    Map(
      true -> value("on"),
      false -> value("off"))
  }

  val loggingLevelPath = "akka.loglevel"
  def loggingLevelValues(level: LogLevel): ConfigValue = {
    level match {
      case ErrorLevel   => value("ERROR")
      case WarningLevel => value("WARNING")
      case InfoLevel    => value("INFO")
      case DebugLevel   => value("DEBUG")
    }
  }

  val serializationBindingsPath = "akka.actor.serialization-bindings"
  def serializationBindingsValues(
    existingValues: Map[String, String])(registrations: List[String]): ConfigValue = {
    val newValues = registrations.map { registration =>
      (registration, "kryo")
    }
    value(existingValues ++ newValues)
  }

  val kryoClassesPath = "akka.actor.kryo.classes"
  def kryoClassesValues(
    existingValues: List[String])(registrations: List[String]): ConfigValue = {
    value(existingValues ++ registrations)
  }

  val kryoInitializerPath = "akka.actor.kryo.kryo-custom-serializer-init"

  val kryoSerializerPoolSizePath = "akka.actor.kryo.serializer-pool-size"

  val hostnamePath = "akka.actor.remote.netty.tcp.hostname"

  val portPath = "akka.actor.remote.netty.tcp.port"

  val backoffRemoteDispatcherPath = "akka.actor.remote.backoff-remote-dispatcher"

  val defaultRemoteDispatcherPath = "akka.actor.remote.default-remote-dispatcher"

  val serverSocketWorkerPoolPath = "akka.actor.remote.netty.tcp.server-socket-worker-pool"

  val clientSocketWorkerPoolPath = "akka.actor.remote.netty.tcp.client-socket-worker-pool"

  def dispatcherConfig(numberOfCores: Int): ConfigObject = {
    ConfigFactory.parseString(s"""
# Dispatcher is the name of the event-based dispatcher
type = Dispatcher

# What kind of ExecutionService to use
executor = "fork-join-executor"

# Configuration for the fork join pool
fork-join-executor {
  # Min number of threads to cap factor-based parallelism number to
  parallelism-min = $numberOfCores
  # Parallelism (threads) ... ceil(available processors * factor)

  # Max number of threads to cap factor-based parallelism number to
  parallelism-max = $numberOfCores
}

# Throughput defines the maximum number of messages to be
# processed per actor before the thread jumps to the next actor.
# Set to 1 for as fair as possible.
throughput = 1000
""").root
  }

  def workerPoolConfig(numberOfCores: Int): ConfigObject = {
    ConfigFactory.parseString(s"""
# Min number of threads to cap factor-based number to
pool-size-min = $numberOfCores

# Max number of threads to cap factor-based number to
pool-size-max = $numberOfCores
""").root
  }

  def config(
    serializeMessages: Option[Boolean],
    loggingLevel: Option[LogLevel],
    kryoRegistrations: List[String],
    kryoInitializer: Option[String],
    hostname: String = InetAddress.getLocalHost.getHostAddress,
    port: Int = 0,
    numberOfCores: Int = Runtime.getRuntime.availableProcessors): Config = {
    val defaults = ConfigFactory.load()
    val configWithParameters = defaults.
      optionalMap(serializeMessagesPath, serializeMessages, serializeMessagesValues).
      optionalMap(loggingLevelPath, loggingLevel, loggingLevelValues _).
      optionalMap(serializationBindingsPath, Some(kryoRegistrations), serializationBindingsValues(defaults.as[Map[String, String]](serializationBindingsPath)) _).
      optionalMap(kryoClassesPath, Some(kryoRegistrations), kryoClassesValues(defaults.as[List[String]](kryoClassesPath)) _).
      optionalMap(kryoInitializerPath, kryoInitializer, value _).
      optionalMap(kryoSerializerPoolSizePath, Some(2 * numberOfCores), value _).
      optionalMap(hostnamePath, Some(hostname), value _).
      optionalMap(portPath, Some(port), value _).
      optionalMap(defaultRemoteDispatcherPath, Some(dispatcherConfig(numberOfCores)), value _).
      optionalMap(backoffRemoteDispatcherPath, Some(dispatcherConfig(numberOfCores)), value _).
      optionalMap(serverSocketWorkerPoolPath, Some(workerPoolConfig(numberOfCores)), value _).
      optionalMap(clientSocketWorkerPoolPath, Some(workerPoolConfig(numberOfCores)), value _)
    configWithParameters
  }

  /**
   * Maps the config if `optionalValue` is defined, returns the original config otherwise.
   */
  private[this] implicit class ExtendedConfig(config: Config) {

    def optionalMap[V](
      path: String,
      optionalValue: Option[V],
      convertValue: V => ConfigValue): Config = {
      optionalValue.map { v =>
        val configValue = convertValue(v)
        val updatedConfig = config.withValue(path, configValue)
        updatedConfig
      }.getOrElse(config)
    }

  }

}
