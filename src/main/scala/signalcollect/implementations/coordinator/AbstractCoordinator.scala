/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

import signalcollect.implementations.graph.DefaultComputationStatistics
import signalcollect.implementations.messaging.AbstractMessageRecipient
import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Worker._
import signalcollect.interfaces.MessageBus._
import signalcollect.interfaces.ComputationStatistics
import signalcollect.implementations.logging.SeparateThreadLogger
import java.util.concurrent.ArrayBlockingQueue
import signalcollect.implementations.messaging.MultiQueue
import signalcollect.interfaces.ComputeGraph
import java.util.concurrent.BlockingQueue
import signalcollect.interfaces._
import signalcollect._
import java.lang.management._
import com.sun.management.OperatingSystemMXBean

abstract class AbstractCoordinator(
  numberOfWorkers: Int,
  workerFactory: WorkerFactory,
  messageInboxFactory: QueueFactory,
  messageBusFactory: MessageBusFactory,
  optionalLogger: Option[MessageRecipient[Any]] = None,
  protected var signalThreshold: Double = 0.01,
  protected var collectThreshold: Double = 0)
  extends AbstractMessageRecipient(messageInboxFactory)
  with ComputeGraph
  with DefaultGraphApi
  with Logging {

  protected val messageBus = messageBusFactory()

  initialize

  protected def initialize {
    Thread.currentThread.setName("Coordinator")
    messageBus.registerCoordinator(this)
    if (optionalLogger.isDefined) {
      messageBus.registerLogger(optionalLogger.get)
    }
    setSignalThreshold(signalThreshold)
    setCollectThreshold(collectThreshold)
  }

  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long = {
    val matchingTypeCounter: (Vertex[_, _]) => Long = { v: Vertex[_, _] =>
      {
        if (m.erasure.isInstance(v)) {
          1l
        } else {
          0l
        }
      }
    }
    customAggregate(0l, { (aggrValue: Long, vertexIncrement: Long) => aggrValue + vertexIncrement }, matchingTypeCounter)
  }

  def sum[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.zero, { (x: N, y: N) => numeric.plus(x, y) })
  }

  def product[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.one, { (x: N, y: N) => numeric.times(x, y) })
  }

  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType] = {
    val stateExtractor: (Vertex[_, _]) => Option[ValueType] = { v: Vertex[_, _] =>
      {
        try {
          Some(v.state.asInstanceOf[ValueType]) // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
        } catch {
          case _ => None
        }
      }
    }
    val optionOperation: (Option[ValueType], Option[ValueType]) => Option[ValueType] = {
      (a, b) =>
        (a,b) match {
          case (Some(x), Some(y)) => Some(operation(x,y))
          case (Some(x), None) => Some(x)
          case (None, Some(y)) => Some(y)
          case (None, None) => None
        }
    }
    customAggregate[Option[ValueType]](None, optionOperation, stateExtractor)
  }
  
  def aggregateStates[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType): ValueType = {
    val stateExtractor: (Vertex[_, _]) => ValueType = { v: Vertex[_, _] =>
      {
        try {
          v.state.asInstanceOf[ValueType] // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
        } catch {
          case _ => neutralElement
        }
      }
    }
    customAggregate(neutralElement, operation, stateExtractor)
  }

  var valueAggregator: Option[WorkerAggregator[_]] = None

  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType = {
    awaitStalledComputation
    pauseComputation
    valueAggregator = Some(new WorkerAggregator[ValueType](numberOfWorkers, neutralElement, operation))
    messageBus.sendToWorkers(CommandAggregate(neutralElement, operation, extractor))
    awaitStalledComputation
    val aggregate = valueAggregator.get()
    if (aggregate.isDefined) {
      aggregate.get.asInstanceOf[ValueType]
    } else {
      throw new Exception("Aggregation error, computation supposedly done, but no aggregated value found.")
    }
  }

  def foreach(f: (Vertex[_, _]) => Unit) {
    awaitStalledComputation
    pauseComputation
    messageBus.sendToWorkers(CommandForEachVertex(f))
    awaitStalledComputation
  }

  def foreach(f: PartialFunction[Vertex[_, _], Unit]) {
    def transformedFunction(v: Vertex[_, _]) {
      if (f.isDefinedAt(v)) {
        f(v)
      }
    }
    foreach(transformedFunction _)
  }

  def process(message: Any) {
    message match {
      case StatusWorkerIsIdle => idle.increment
      case StatusWorkerIsBusy => idle.decrement
      case StatusWorkerHasPaused => paused.increment
      case StatusNumberOfVertices(v) => numberOfVertices.aggregate(v)
      case StatusNumberOfEdges(e) => numberOfEdges.aggregate(e)
      case StatusSignalStepDone => signalStep.increment
      case StatusCollectStepDone(toSignal) => collectStep.aggregate(toSignal)
      case StatusAggregatedValue(value) =>
        if (valueAggregator.isDefined) {
          valueAggregator.get.tryAggregate(value)
        }
      case stats: ComputationProgressStats =>
        if (!computationProgressStatistics.isDone) {
          computationProgressStatistics.aggregate(stats)
        } else if (!computationProgressStatisticsSecondPass.isDone) {
          computationProgressStatisticsSecondPass.aggregate(stats)
        } else {
          computationProgressStatistics = computationProgressStatisticsSecondPass
          computationProgressStatisticsSecondPass = new WorkerAggregator[ComputationProgressStats](numberOfWorkers, ComputationProgressStats(), (_ + _))
          computationProgressStatisticsSecondPass.aggregate(stats)
        }
        val firstPass = computationProgressStatistics
        val secondPass = computationProgressStatisticsSecondPass
        def noOperationsPending = firstPass().get.collectOperationsPending == 0 && firstPass().get.signalOperationsPending == 0
        computationStalled = firstPass.isDone && secondPass.isDone && (firstPass().get equals secondPass().get) && (!computationInProgress || noOperationsPending)
    }
  }

  def shutDown {
    messageBus.sendToWorkers(CommandShutDown)
    if (optionalLogger.isDefined && optionalLogger.get.isInstanceOf[SeparateThreadLogger]) {
      messageBus.sendToLogger(CommandShutDown)
    }
  }

  val signalStep = new Counter(numberOfWorkers)
  val collectStep = new LongAggregator(numberOfWorkers)
  val idle = new Counter(numberOfWorkers)
  val paused = new Counter(numberOfWorkers)
  val numberOfVertices = new LongAggregator(numberOfWorkers)
  val numberOfEdges = new LongAggregator(numberOfWorkers)

  var computationProgressStatistics = new WorkerAggregator[ComputationProgressStats](numberOfWorkers, ComputationProgressStats(), (_ + _))
  var computationProgressStatisticsSecondPass = new WorkerAggregator[ComputationProgressStats](numberOfWorkers, ComputationProgressStats(), (_ + _))

  var computationStalled: Boolean = false
  var computationInProgress = false

  def setSignalThreshold(t: Double) {
    signalThreshold = t
    messageBus.sendToWorkers(CommandSetSignalThreshold(t))
  }

  def setCollectThreshold(t: Double) {
    collectThreshold = t
    messageBus.sendToWorkers(CommandSetCollectThreshold(t))
  }

  def setStepsLimit(l: Int) {
    throw new UnsupportedOperationException("Steps limit not supported yet.")
  }

  def awaitIdle: Long = {
    val startTime = System.nanoTime
    awaitStalledComputation
    val stopTime = System.nanoTime
    stopTime - startTime
  }
  
  def execute: ComputationStatistics = {
    log("Waiting for graph loading to finish ...")
    
    val graphLoadingWait = awaitIdle
    
    log("Starting computation ...")
    val jvmCpuStartTime = getJVMCpuTime
    val startTime = System.nanoTime

    /*******************************/
    val statsMap = performComputation
    /*******************************/

    val stopTime = System.nanoTime
    val jvmCpuStopTime = getJVMCpuTime
    val totalTime: Long = stopTime - startTime
    val totalJvmCpuTime: Long = jvmCpuStopTime - jvmCpuStartTime
    log("\t\t\tDONE")

    //  	statsMap.put("stepsLimit", stepsLimit)
    statsMap.put("signalThreshold", signalThreshold)
    statsMap.put("collectThreshold", collectThreshold)
    statsMap.put("numberOfWorkers", numberOfWorkers)
    statsMap.put("computeGraph", computeGraphName)
    statsMap.put("worker", workerName)
    statsMap.put("messageBus", messageBusName)
    statsMap.put("messageInbox", messageInboxName)
    statsMap.put("loggerName", loggerName)
    statsMap.put("numberOfVertices", numberOfVertices().getOrElse(-1l))
    statsMap.put("numberOfEdges", numberOfEdges().getOrElse(-1l))

    val progressStats = computationProgressStatistics().get
    statsMap.put("vertexCollectOperations", progressStats.collectOperationsExecuted)
    statsMap.put("vertexSignalOperations", progressStats.signalOperationsExecuted)
    statsMap.put("numberOfVertices", progressStats.verticesAdded - progressStats.verticesRemoved)
    statsMap.put("numberOfEdges", progressStats.outgoingEdgesAdded - progressStats.outgoingEdgesRemoved)
    statsMap.put("graphLoadingWaitInMilliseconds", (graphLoadingWait / 1000000.0).toLong)
    statsMap.put("jvmCpuTimeInMilliseconds", (totalJvmCpuTime / 1000000.0).toLong)
    statsMap.put("computationTimeInMilliseconds", (totalTime / 1000000.0).toLong)
    new DefaultComputationStatistics(statsMap)
  }

  def performComputation: collection.mutable.Map[String, Any]

  protected val workers: Seq[Worker] = createWorkers

  protected def createWorkers: Array[Worker] = {
    val workers = new Array[Worker](numberOfWorkers)
    for (i <- 0 until numberOfWorkers) {
      val worker = workerFactory(messageBus, messageInboxFactory, Storage.defaultFactory)
      messageBus.registerWorker(i, worker)
      new Thread(worker, "Worker#" + i).start
      workers(i) = worker
    }
    workers
  }

  def pauseComputation {
    paused.reset
    messageBus.sendToWorkers(CommandPauseComputation)
    while (!paused.isDone) {
      handleMessage
    }
    computationInProgress = false
  }

  def startComputation {
    messageBus.sendToWorkers(CommandStartComputation)
    computationInProgress = true
  }

  def awaitStalledComputation {
    computationStalled = false
    computationProgressStatistics.reset
    computationProgressStatisticsSecondPass.reset
    var firstPassInitiated = false
    var secondPassInitiated = false
    def firstPassInProgress = firstPassInitiated && !computationProgressStatistics.isDone
    def secondPassInProgress = secondPassInitiated && !computationProgressStatisticsSecondPass.isDone
    def passInProgress = firstPassInProgress || secondPassInProgress
    while (!computationStalled) {
      if (idle.isDone) {
        if (!passInProgress) {
          messageBus.sendToWorkers(CommandSendComputationProgressStats)
          if (!firstPassInitiated) {
            firstPassInitiated = true
          } else {
            secondPassInitiated = true
          }
        }
      }
      handleMessage
    }
  }

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }

  lazy val computeGraphName = getClass.getSimpleName
  lazy val workerName = workerFactory(messageBusFactory(), messageInboxFactory, Storage.defaultFactory).getClass.getSimpleName
  lazy val messageBusName = messageBusFactory().getClass.getSimpleName
  lazy val messageInboxName = messageInboxFactory().getClass.getSimpleName
  lazy val loggerName = optionalLogger.getClass.getSimpleName

}