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

import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.api.Queues._
import signalcollect.api.Workers._
import signalcollect.api.MessageBuses._
import signalcollect.api.ComputationStatistics
import signalcollect.implementations.logging.SeparateThreadLogger
import java.util.concurrent.ArrayBlockingQueue
import signalcollect.implementations.messaging.MultiQueue
import signalcollect.api.ComputeGraph
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
  optionalLogger: Option[MessageRecipient[Any]] = None)
  extends implementations.messaging.MessageRecipient(messageInboxFactory)
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
  }

  def foreach(f: (interfaces.Vertex[_, _]) => Unit) {
    awaitStalledComputation
    pauseComputation
    messageBus.sendToWorkers(CommandForEachVertex(f))
    awaitStalledComputation
    startComputation
    awaitStalledComputation
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

  def execute(signalThreshold: Double = 0.001, collectThreshold: Double = 0, stepsLimit: Int = 100000): ComputationStatistics = {
    log("Initializing graph ...")
    initializeGraph(signalThreshold, collectThreshold)
    log("\t\t\tDONE")

    /*******************************/
    log("Waiting for graph loading to finish ...")
    awaitStalledComputation
    log("\t\t\tDONE")
    /*******************************/

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

    statsMap.put("jvmCpuTime", (totalJvmCpuTime / 1000000.0).toLong)
    statsMap.put("computationTimeInMilliseconds", (totalTime / 1000000.0).toLong)
    new ComputationStatistics(statsMap)
  }

  def performComputation: collection.mutable.Map[String, Any]

  protected val workers: Seq[Worker] = createWorkers

  protected def createWorkers: Array[Worker] = {
    val workers = new Array[Worker](numberOfWorkers)
    for (i <- 0 until numberOfWorkers) {
      val worker = workerFactory(messageBus, messageInboxFactory)
      messageBus.registerWorker(i, worker)
      new Thread(worker, "Worker#" + i).start
      workers(i) = worker
    }
    workers
  }

  def initializeGraph(signalThreshold: Double, collectThreshold: Double) {
    messageBus.sendToWorkers(CommandSetSignalThreshold(signalThreshold))
    messageBus.sendToWorkers(CommandSetCollectThreshold(collectThreshold))
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
  lazy val workerName = workerFactory(messageBusFactory(), messageInboxFactory).getClass.getSimpleName
  lazy val messageBusName = messageBusFactory().getClass.getSimpleName
  lazy val messageInboxName = messageInboxFactory().getClass.getSimpleName
  lazy val loggerName = optionalLogger.getClass.getSimpleName

}