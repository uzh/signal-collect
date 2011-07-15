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

package signalcollect.implementations.coordinator

import signalcollect.interfaces._
import signalcollect.configuration._
import signalcollect.implementations.messaging._
import signalcollect.api.Factory._
import java.lang.reflect.Method
import signalcollect.implementations.graph.DefaultGraphApi
import scala.collection.parallel.mutable.ParArray
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import java.util.concurrent.atomic.AtomicLong
import signalcollect.api.Factory
import signalcollect.configuration.bootstrap._

class WorkerApi(config: Configuration, logger: MessageRecipient[LogMessage]) extends MessageRecipient[Any] with DefaultGraphApi with Logging {

  protected lazy val workerFactory = {
    config.bootstrapConfiguration.executionArchitecture match {
      case LocalExecutionArchitecture => Factory.Worker.Local
      //case DistributedExecutionArchitecture => Factory.Worker.Akka
      case other => throw new Exception("Currently only local workers supported.")
    }
  }

  override def toString = "WorkerApi"
  
  // initialize workers array
  protected val workers = new Array[Worker](config.numberOfWorkers)

  /**
   * Individual worker creation
   * It creates and initializes the workers whether remote or local
   *
   * @param: workerId is the id of the worker
   */
  def createWorker(workerId: Int) {

    // create worker message bus from configuration
    val workerMessageBus = config.graphConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)

    // create the worker configuration for worker instantiation
    config.workerConfigurations.put(workerId, DefaultWorkerConfiguration(messageBus = workerMessageBus, storageFactory = config.graphConfiguration.storageFactory))

    // register the coordinator
    workerMessageBus.registerCoordinator(this)

    // create thw worker
    val worker = workerFactory.createInstance(workerId, config.workerConfigurations(workerId))

    // initialize it
    worker.initialize

    // put it to the array of workers
    workers(workerId) = worker

  }

  //protected val workers: Array[Worker] = createWorkers

  protected lazy val workerProxies: Array[Worker] = createWorkerProxies
  protected lazy val workerProxyMessageBuses: Array[MessageBus[Any]] = createWorkerProxyMessageBuses
  protected lazy val parallelWorkerProxies = workerProxies.par
  protected lazy val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)
  protected lazy val messageBus = config.graphConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
  protected lazy val workerStatusMap = new ConcurrentHashMap[Int, WorkerStatus]()
  protected val messagesReceived = new AtomicLong(0l)
  protected val statusMonitor = new Object

  var signalSteps = 0l
  var collectSteps = 0l

  protected def createWorkerProxyMessageBuses: Array[MessageBus[Any]] = {
    val workerProxyMessageBuses = new Array[MessageBus[Any]](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val proxyMessageBus = config.graphConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
      proxyMessageBus.registerCoordinator(this)
      workerProxyMessageBuses(workerId) = proxyMessageBus
    }
    workerProxyMessageBuses
  }

  protected def createWorkerProxies: Array[Worker] = {
    val workerProxies = new Array[Worker](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val workerProxy = WorkerProxy.create(workerId, workerProxyMessageBuses(workerId))
      workerProxies(workerId) = workerProxy
    }
    workerProxies
  }

  // initialize

  var isInitialized = false

  def initialize {

    if (isInitialized)
      return

    Thread.currentThread.setName("Coordinator")
    messageBus.registerCoordinator(this)
    workerProxyMessageBuses foreach (_.registerCoordinator(this))
    for (workerId <- 0 until config.numberOfWorkers) {
      messageBus.registerWorker(workerId, workers(workerId))
      workerProxyMessageBuses foreach (_.registerWorker(workerId, workers(workerId)))
    }
    for (workerId <- 0 until config.numberOfWorkers) {
      parallelWorkerProxies foreach (_.registerWorker(workerId, workers(workerId)))
    }

    isInitialized = true

  }

  def getWorkerStatistics: List[WorkerStatistics] = {
    parallelWorkerProxies.map(_.getWorkerStatistics).toList
  }

  def receive(message: Any) {
    this.synchronized {
      if (!message.isInstanceOf[LogMessage]) {
        messagesReceived.incrementAndGet
      }
      message match {
        case r: WorkerReply =>
          workerProxies(r.workerId).receive(message)
        case ws: WorkerStatus =>
          workerStatusMap.put(ws.workerId, ws)
          statusMonitor.synchronized {
            statusMonitor.notify
          }
        case l: LogMessage =>
          logger.receive(l)
        case other => println("Received unknown message: " + other)
      }
    }
  }

  def messagesSentByWorkers = workerStatusMap.values.foldLeft(0l)(_ + _.messagesSent) + workerStatusMap.size // the status message that was sent was not yet counted by the worker
  def messagesSentByWorkerProxies = workerProxyMessageBuses.foldLeft(0l)(_ + _.messagesSent)
  def messagesSentByCoordinator = messageBus.messagesSent

  def messagesReceivedByWorkers = workerStatusMap.values.foldLeft(0l)(_ + _.messagesReceived)
  def messagesReceivedByCoordinator = messagesReceived.get

  def totalMessagesSent: Long = messagesSentByWorkers + messagesSentByWorkerProxies + messagesSentByCoordinator
  def totalMessagesReceived: Long = messagesReceivedByWorkers + messagesReceivedByCoordinator

  def idle: Boolean = workerStatusMap.values.forall(_.isIdle) && totalMessagesSent == totalMessagesReceived

  def awaitIdle: Long = {
    val startTime = System.nanoTime
    statusMonitor.synchronized {
      while (!idle) {
        statusMonitor.wait(10)
      }
    }
    val stopTime = System.nanoTime
    stopTime - startTime
  }

  def paused: Boolean = workerStatusMap.values.forall(_.isPaused)

  def awaitPaused {
    statusMonitor.synchronized {
      while (!paused) {
        statusMonitor.wait(10)
      }
    }
  }

  def started: Boolean = workerStatusMap.values.forall(!_.isPaused)

  def awaitStarted {
    statusMonitor.synchronized {
      while (!started) {
        statusMonitor.wait(10)
      }
    }
  }

  def signalStep = {
    signalSteps += 1
    parallelWorkerProxies foreach (_.signalStep)
  }

  def collectStep: Boolean = {
    collectSteps += 1
    parallelWorkerProxies.map(_.collectStep).fold(true)(_ && _)
  }

  def startComputation {
    parallelWorkerProxies.foreach(_.startComputation)
    awaitStarted
  }

  def pauseComputation {
    parallelWorkerProxies.foreach(_.pauseComputation)
    awaitPaused
  }

  def recalculateScores = parallelWorkerProxies foreach (_.recalculateScores)

  def recalculateScoresForVertexWithId(vertexId: Any) = workerProxies(mapper.getWorkerIdForVertexId(vertexId)).recalculateScoresForVertexWithId(vertexId)

  def shutdown = parallelWorkerProxies foreach (_.shutdown)

  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    workerProxies(mapper.getWorkerIdForVertexId(vertexId)).forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex[_, _]) => Unit) = parallelWorkerProxies foreach (_.foreachVertex(f))

  def customAggregate[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType,
    extractor: (Vertex[_, _]) => ValueType): ValueType = {
    val aggregateArray: ParArray[ValueType] = parallelWorkerProxies map (_.aggregate(neutralElement, operation, extractor))
    aggregateArray.fold(neutralElement)(operation(_, _))
  }

  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit) = parallelWorkerProxies foreach (_.setUndeliverableSignalHandler(h))

  def setSignalThreshold(t: Double) = parallelWorkerProxies foreach (_.setSignalThreshold(t))

  def setCollectThreshold(t: Double) = parallelWorkerProxies foreach (_.setCollectThreshold(t))

}