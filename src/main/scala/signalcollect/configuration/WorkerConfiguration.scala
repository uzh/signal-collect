package signalcollect.configuration

import signalcollect.api._
import signalcollect.interfaces._
import signalcollect.implementations.messaging.DefaultMessageBus

/**
 * Generalization of worker configuration parameters for the worker constructor
 */
trait WorkerConfiguration {
  def messageBus: MessageBus[Any, Any]
  def storageFactory: StorageFactory
}

trait RemoteWorkerConfiguration extends WorkerConfiguration {
  def ipAddress: String
  def port: Int
}

case class DefaultWorkerConfiguration(
  messageBus: MessageBus[Any, Any],
  storageFactory: StorageFactory = Factory.Storage.InMemory) extends WorkerConfiguration

case class DefaultRemoteWorkerConfiguration(
  messageBus: MessageBus[Any, Any],
  storageFactory: StorageFactory = Factory.Storage.InMemory,
  ipAddress: String = "localhost",
  port: Int = 0) extends RemoteWorkerConfiguration