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
import signalcollect.implementations.messaging._
import signalcollect.api.Factory._
import java.lang.reflect.Method
import signalcollect.implementations.graph.DefaultGraphApi
import scala.collection.parallel.mutable.ParArray
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import java.util.concurrent.atomic.AtomicLong
import signalcollect.api.Factory

class WorkerApi(config: Configuration) extends MessageRecipient[Any] with DefaultGraphApi with Logging {

  protected lazy val workerFactory = {
    config.executionMode match {
      case AsynchronousExecutionMode => Factory.Worker.Asynchronous
      case SynchronousExecutionMode => Factory.Worker.Synchronous
    }
  }
  protected val workers: Array[Worker] = createWorkers
  protected lazy val workerProxies: Array[Worker] = createWorkerProxies
  protected lazy val workerProxyMessageBuses: Array[MessageBus[Any, Any]] = createWorkerProxyMessageBuses
  protected lazy val parallelWorkerProxies = workerProxies.par
  protected lazy val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)
  protected lazy val messageBus = config.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
  protected lazy val workerStatusMap = new ConcurrentHashMap[Int, WorkerStatus]()
  protected val messagesReceived = new AtomicLong(0l)
  protected val statusMonitor = new Object

  var signalSteps = 0l
  var collectSteps = 0l
 
  protected def createWorkers: Array[Worker] = {
    val workers = new Array[Worker](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val workerMessageBus = config.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
      workerMessageBus.registerCoordinator(this)
      val worker = workerFactory.createInstance(workerId, workerMessageBus, config.storageFactory)
      worker.initialize
      workers(workerId) = worker
    }
    workers
  }

  protected def createWorkerProxyMessageBuses: Array[MessageBus[Any, Any]] = {
    val workerProxyMessageBuses = new Array[MessageBus[Any, Any]](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val proxyMessageBus = config.messageBusFactory.createInstance(config.numberOfWorkers, mapper)
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

  initialize

  protected def initialize {
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
  }

  def getWorkerStatistics: List[WorkerStatistics] = {
    parallelWorkerProxies.map(_.getWorkerStatistics).toList
  }
  
  def receive(message: Any) {
    this.synchronized {
      messagesReceived.incrementAndGet
      message match {
        case r: WorkerReply =>
          workerProxies(r.workerId).receive(message)
        case ws: WorkerStatus =>
          workerStatusMap.put(ws.workerId, ws)
          statusMonitor.synchronized {
            statusMonitor.notify
          }
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

  def registerLogger(l: MessageRecipient[Any]) {
    messageBus.registerLogger(l)
    workerProxyMessageBuses foreach (_.registerLogger(l))
    parallelWorkerProxies foreach (_.registerLogger(l))
  }
  
  def logCoordinatorMessage(m: Any) = log(m)
  
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

  def recalculateScoresForVertexId(vertexId: Any) = workerProxies(mapper.getWorkerIdForVertexId(vertexId)).recalculateScoresForVertexId(vertexId)

  def shutdown = parallelWorkerProxies foreach (_.shutdown)

  def forVertexWithId(vertexId: Any, f: (Vertex[_, _]) => Unit) = workerProxies(mapper.getWorkerIdForVertexId(vertexId)).forVertexWithId(vertexId, f)

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