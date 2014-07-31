package com.signalcollect.worker

import com.signalcollect.interfaces._
import akka.event.LoggingAdapter

final class IntDoubleWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Int, Double],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Int, Double],
  schedulerFactory: SchedulerFactory[Int, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Double],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Int, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class IntDoubleAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Int, Double],
  mapperFactory: MapperFactory[Int],
  storageFactory: StorageFactory[Int, Double],
  schedulerFactory: SchedulerFactory[Int, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Double],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Int, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new IntDoubleWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class IntFloatWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Int, Float],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Int, Float],
  schedulerFactory: SchedulerFactory[Int, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Float],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Int, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class IntFloatAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Int, Float],
  mapperFactory: MapperFactory[Int],
  storageFactory: StorageFactory[Int, Float],
  schedulerFactory: SchedulerFactory[Int, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Float],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Int, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new IntFloatWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class IntIntWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Int, Int],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Int, Int],
  schedulerFactory: SchedulerFactory[Int, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Int],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Int, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class IntIntAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Int, Int],
  mapperFactory: MapperFactory[Int],
  storageFactory: StorageFactory[Int, Int],
  schedulerFactory: SchedulerFactory[Int, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Int],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Int, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new IntIntWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class IntLongWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Int, Long],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Int, Long],
  schedulerFactory: SchedulerFactory[Int, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Long],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Int, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class IntLongAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Int, Long],
  mapperFactory: MapperFactory[Int],
  storageFactory: StorageFactory[Int, Long],
  schedulerFactory: SchedulerFactory[Int, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Long],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Int, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new IntLongWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class LongDoubleWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Long, Double],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Long, Double],
  schedulerFactory: SchedulerFactory[Long, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Double],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Long, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class LongDoubleAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Long, Double],
  mapperFactory: MapperFactory[Long],
  storageFactory: StorageFactory[Long, Double],
  schedulerFactory: SchedulerFactory[Long, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Double],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Long, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new LongDoubleWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class LongFloatWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Long, Float],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Long, Float],
  schedulerFactory: SchedulerFactory[Long, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Float],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Long, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class LongFloatAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Long, Float],
  mapperFactory: MapperFactory[Long],
  storageFactory: StorageFactory[Long, Float],
  schedulerFactory: SchedulerFactory[Long, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Float],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Long, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new LongFloatWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class LongIntWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Long, Int],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Long, Int],
  schedulerFactory: SchedulerFactory[Long, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Int],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Long, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class LongIntAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Long, Int],
  mapperFactory: MapperFactory[Long],
  storageFactory: StorageFactory[Long, Int],
  schedulerFactory: SchedulerFactory[Long, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Int],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Long, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new LongIntWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class LongLongWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Long, Long],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Long, Long],
  schedulerFactory: SchedulerFactory[Long, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Long],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Long, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class LongLongAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Long, Long],
  mapperFactory: MapperFactory[Long],
  storageFactory: StorageFactory[Long, Long],
  schedulerFactory: SchedulerFactory[Long, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Long],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Long, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new LongLongWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class AnyDoubleWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Any, Double],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Any, Double],
  schedulerFactory: SchedulerFactory[Any, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Double],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Any, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class AnyDoubleAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Any, Double],
  mapperFactory: MapperFactory[Any],
  storageFactory: StorageFactory[Any, Double],
  schedulerFactory: SchedulerFactory[Any, Double],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Double],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Double],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Double],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Any, Double](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new AnyDoubleWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class AnyFloatWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Any, Float],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Any, Float],
  schedulerFactory: SchedulerFactory[Any, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Float],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Any, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class AnyFloatAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Any, Float],
  mapperFactory: MapperFactory[Any],
  storageFactory: StorageFactory[Any, Float],
  schedulerFactory: SchedulerFactory[Any, Float],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Float],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Float],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Float],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Any, Float](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new AnyFloatWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class AnyIntWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Any, Int],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Any, Int],
  schedulerFactory: SchedulerFactory[Any, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Int],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Any, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class AnyIntAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Any, Int],
  mapperFactory: MapperFactory[Any],
  storageFactory: StorageFactory[Any, Int],
  schedulerFactory: SchedulerFactory[Any, Int],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Int],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Int],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Int],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Any, Int](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new AnyIntWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class AnyLongWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Any, Long],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Any, Long],
  schedulerFactory: SchedulerFactory[Any, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Long],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Any, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class AnyLongAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Any, Long],
  mapperFactory: MapperFactory[Any],
  storageFactory: StorageFactory[Any, Long],
  schedulerFactory: SchedulerFactory[Any, Long],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Any, Long],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Any, Long],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Any, Long],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Any, Long](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new AnyLongWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class IntAnyWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Int, Any],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Int, Any],
  schedulerFactory: SchedulerFactory[Int, Any],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Any],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Any],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Any],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Int, Any](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class IntAnyAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Int, Any],
  mapperFactory: MapperFactory[Int],
  storageFactory: StorageFactory[Int, Any],
  schedulerFactory: SchedulerFactory[Int, Any],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Int, Any],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Int, Any],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Int, Any],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Int, Any](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new IntAnyWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}

final class LongAnyWorkerImplementation(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  eagerIdleDetection: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean,
  messageBus: MessageBus[Long, Any],
  log: LoggingAdapter,
  storageFactory: StorageFactory[Long, Any],
  schedulerFactory: SchedulerFactory[Long, Any],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Any],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Any],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Any],
  signalThreshold: Double,
  collectThreshold: Double) extends WorkerImplementation[Long, Any](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  eagerIdleDetection,
  supportBlockingGraphModificationsInVertex,
  messageBus,
  log,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  signalThreshold,
  collectThreshold)

final class LongAnyAkkaWorker(
  workerId: Int,
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Long, Any],
  mapperFactory: MapperFactory[Long],
  storageFactory: StorageFactory[Long, Any],
  schedulerFactory: SchedulerFactory[Long, Any],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Long, Any],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Long, Any],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Long, Any],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) extends AkkaWorker[Long, Any](
  workerId,
  numberOfWorkers,
  numberOfNodes,
  messageBusFactory,
  mapperFactory,
  storageFactory,
  schedulerFactory,
  existingVertexHandlerFactory,
  undeliverableSignalHandlerFactory,
  edgeAddedToNonExistentVertexHandlerFactory,
  heartbeatIntervalInMilliseconds,
  eagerIdleDetection,
  throttlingEnabled,
  supportBlockingGraphModificationsInVertex) {

  override val worker = new LongAnyWorkerImplementation(
    workerId = workerId,
    numberOfWorkers = numberOfWorkers,
    numberOfNodes = numberOfNodes,
    eagerIdleDetection = eagerIdleDetection,
    supportBlockingGraphModificationsInVertex = supportBlockingGraphModificationsInVertex,
    messageBus = messageBus,
    log = log,
    storageFactory = storageFactory,
    schedulerFactory = schedulerFactory,
    existingVertexHandlerFactory = existingVertexHandlerFactory,
    undeliverableSignalHandlerFactory = undeliverableSignalHandlerFactory,
    edgeAddedToNonExistentVertexHandlerFactory = edgeAddedToNonExistentVertexHandlerFactory,
    signalThreshold = 0.01,
    collectThreshold = 0.0)
}
