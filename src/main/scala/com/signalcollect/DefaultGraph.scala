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

package com.signalcollect

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import scala.Array.canBuildFrom
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.reflect.ClassTag

import com.signalcollect.configuration.ActorSystemRegistry
import com.signalcollect.configuration.AkkaConfig
import com.signalcollect.configuration.EventBased
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.configuration.Pinned
import com.signalcollect.configuration.TerminationReason
import com.signalcollect.console.Execution
import com.signalcollect.console.ConsoleServer
import com.signalcollect.coordinator.DefaultCoordinator
import com.signalcollect.coordinator.IsIdle
import com.signalcollect.coordinator.OnIdle
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.EdgeId
import com.signalcollect.interfaces.LogMessage
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageRecipientRegistry
import com.signalcollect.interfaces.Severe
import com.signalcollect.interfaces.WorkerActor
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.logging.DefaultLogger
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.messaging.DefaultVertexToWorkerMapper
import com.sun.management.OperatingSystemMXBean

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.japi.Creator
import akka.pattern.ask
import akka.util.Timeout
import com.signalcollect.coordinator.OnIdle
import com.signalcollect.coordinator.IsIdle
import scala.language.postfixOps
import com.signalcollect.interfaces.ComplexAggregation
import java.net.InetSocketAddress

/**
 * Creator in separate class to prevent excessive closure-capture of the DefaultGraph class (Error[java.io.NotSerializableException DefaultGraph])
 */
case class WorkerCreator[Id: ClassTag, Signal: ClassTag](workerId: Int, workerFactory: WorkerFactory, numberOfWorkers: Int, config: GraphConfiguration) extends Creator[WorkerActor[Id, Signal]] {
  def create: WorkerActor[Id, Signal] = workerFactory.createInstance[Id, Signal](workerId, numberOfWorkers, config)
}

/**
 * Creator in separate class to prevent excessive closure-capture of the DefaultGraph class (Error[java.io.NotSerializableException DefaultGraph])
 */
case class CoordinatorCreator[Id: ClassTag, Signal: ClassTag](numberOfWorkers: Int, messageBusFactory: MessageBusFactory, heartbeatIntervalInMilliseconds: Long, loggingLevel: Int) extends Creator[DefaultCoordinator[Id, Signal]] {
  def create: DefaultCoordinator[Id, Signal] = new DefaultCoordinator[Id, Signal](numberOfWorkers, messageBusFactory, heartbeatIntervalInMilliseconds, loggingLevel)
}

/**
 * Creator in separate class to prevent excessive closure-capture of the DefaultGraph class (Error[java.io.NotSerializableException DefaultGraph])
 */
case class LoggerCreator(loggingFunction: LogMessage => Unit) extends Creator[DefaultLogger] {
  def create: DefaultLogger = new DefaultLogger(loggingFunction)
}

/**
 * Default graph implementation.
 *
 * Provisions the resources and initializes the workers and the coordinator.
 */
class DefaultGraph[@specialized(Int, Long) Id: ClassTag, @specialized(Int, Long, Float, Double) Signal: ClassTag](
  val config: GraphConfiguration = GraphConfiguration()) extends Graph[Id, Signal] {

  val console = {
    if (config.consoleEnabled) {
      new ConsoleServer[Id](config.consoleHttpPort)
    } else {
      null
    }
  }

  val akkaConfig = AkkaConfig.get(config.akkaMessageCompression, config.loggingLevel)
  override def toString: String = "DefaultGraph"

  val system: ActorSystem = ActorSystem("SignalCollect", akkaConfig)
  ActorSystemRegistry.register(system)

  val nodes = config.nodeProvisioner.getNodes(akkaConfig)

  val numberOfWorkers = nodes.par map (_.numberOfCores) sum

  val mapper = new DefaultVertexToWorkerMapper(numberOfWorkers)

  val workerActors: Array[ActorRef] = {
    val actors = new Array[ActorRef](numberOfWorkers)
    var workerId = 0
    for (node <- nodes) {
      for (core <- 0 until node.numberOfCores) {
        val workerCreator = WorkerCreator[Id, Signal](workerId, config.workerFactory, numberOfWorkers, config)
        val workerName = node.createWorker(workerId, config.akkaDispatcher, workerCreator.create _)
        actors(workerId) = system.actorFor(workerName)
        workerId += 1
      }
    }
    actors
  }

  val coordinatorActor: ActorRef = {
    val coordinatorCreator = CoordinatorCreator[Id, Signal](numberOfWorkers, config.messageBusFactory, config.heartbeatIntervalInMilliseconds, config.loggingLevel)
    config.akkaDispatcher match {
      case EventBased => system.actorOf(Props[DefaultCoordinator[Id, Signal]].withCreator(coordinatorCreator.create), name = "Coordinator")
      case Pinned => system.actorOf(Props[DefaultCoordinator[Id, Signal]].withCreator(coordinatorCreator.create).withDispatcher("akka.actor.pinned-dispatcher"), name = "Coordinator")
    }
  }

  if (console != null) { console.setCoordinator(coordinatorActor) }

  val loggerActor: ActorRef = {
    val loggerCreator = LoggerCreator(config.logger)
    system.actorOf(Props[DefaultLogger].withCreator(loggerCreator.create()), name = "Logger")
  }

  val bootstrapWorkerProxies = workerActors map (AkkaProxy.newInstance[WorkerActor[Id, Signal]](_))
  val coordinatorProxy = AkkaProxy.newInstance[Coordinator[Id, Signal]](coordinatorActor)

  initializeMessageBuses

  def initializeMessageBuses {
    val registries: List[MessageRecipientRegistry] = coordinatorProxy :: bootstrapWorkerProxies.toList
    for (registry <- registries.par) {
      try {
        registry.registerCoordinator(coordinatorActor)
      } catch {
        case e: Exception => loggerActor ! Severe("Exception in `initializeMessageBuses`:" + e.getCause + "\n" + e, this.toString)
      }
      for (workerId <- (0 until numberOfWorkers).par) {
        registry.registerWorker(workerId, workerActors(workerId))
      }
      registry.registerLogger(loggerActor)
    }
  }

  lazy val workerApi = coordinatorProxy.getWorkerApi
  lazy val graphEditor = coordinatorProxy.getGraphEditor

  /** GraphApi */

  def execute: ExecutionInformation = execute(ExecutionConfiguration)

  /**
   * Returns the time it took to execute `operation`
   *
   * @note Only works if operation is blocking
   */
  def measureTime(operation: () => Unit): Duration = {
    val startTime = System.nanoTime
    operation()
    val stopTime = System.nanoTime
    new FiniteDuration(stopTime - startTime, TimeUnit.NANOSECONDS)
  }

  def execute(parameters: ExecutionConfiguration): ExecutionInformation = {
    val executionStartTime = System.nanoTime
    val stats = ExecutionStatistics()
    stats.graphIdleWaitingTime = measureTime(awaitIdle _)
    workerApi.setSignalThreshold(parameters.signalThreshold)
    workerApi.setCollectThreshold(parameters.collectThreshold)
    val jvmCpuStartTime = getJVMCpuTime
    parameters.executionMode match {
      case ExecutionMode.Synchronous =>
        stats.computationTime = measureTime(() => synchronousExecution(stats, parameters.timeLimit, parameters.stepsLimit, parameters.globalTerminationCondition))
      case ExecutionMode.OptimizedAsynchronous =>
        stats.computationTime = measureTime(() => optimizedAsynchronousExecution(stats, parameters.timeLimit, parameters.globalTerminationCondition))
      case ExecutionMode.PureAsynchronous =>
        stats.computationTime = measureTime(() => pureAsynchronousExecution(stats, parameters.timeLimit, parameters.globalTerminationCondition))
      case ExecutionMode.ContinuousAsynchronous =>
        workerApi.startComputation
        stats.terminationReason = TerminationReason.Ongoing
      case ExecutionMode.Interactive =>
        new InteractiveExecution[Id](this, console,stats, parameters.timeLimit, parameters.stepsLimit, parameters.globalTerminationCondition).run()
    }
    stats.jvmCpuTime = new FiniteDuration(getJVMCpuTime - jvmCpuStartTime, TimeUnit.NANOSECONDS)
    val executionStopTime = System.nanoTime
    stats.totalExecutionTime = new FiniteDuration(executionStopTime - executionStartTime, TimeUnit.NANOSECONDS)
    val workerStatistics = workerApi.getIndividualWorkerStatistics
    ExecutionInformation(config, numberOfWorkers, nodes map (_.numberOfCores.toString), parameters, stats, workerStatistics.fold(WorkerStatistics(null))(_ + _), workerStatistics)
  }

  protected def synchronousExecution(
    stats: ExecutionStatistics,
    timeLimit: Option[Long],
    stepsLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]) {
    var converged = false
    var globalTermination = false
    var interval = 0l
    if (globalTerminationCondition.isDefined) {
      interval = globalTerminationCondition.get.aggregationInterval
    }
    val startTime = System.nanoTime
    val nanosecondLimit = timeLimit.getOrElse(0l) * 1000000l
    while (!converged && !isTimeLimitReached && !isStepsLimitReached && !globalTermination) {
      workerApi.signalStep
      awaitIdle
      stats.signalSteps += 1
      converged = workerApi.collectStep
      stats.collectSteps += 1
      if (shouldCheckGlobalCondition) {
        globalTermination = isGlobalTerminationConditionMet(globalTerminationCondition.get)
      }
    }
    if (converged) {
      stats.terminationReason = TerminationReason.Converged
    } else if (globalTermination) {
      stats.terminationReason = TerminationReason.GlobalConstraintMet
    } else if (isStepsLimitReached) {
      stats.terminationReason = TerminationReason.ComputationStepLimitReached
    } else {
      stats.terminationReason = TerminationReason.TimeLimitReached
    }
    def shouldCheckGlobalCondition = interval > 0 && stats.collectSteps % interval == 0
    def isGlobalTerminationConditionMet[ResultType](gtc: GlobalTerminationCondition[ResultType]): Boolean = {
      val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
      gtc.shouldTerminate(globalAggregateValue)
    }
    def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
    def isTimeLimitReached = timeLimit.isDefined && remainingTimeLimit <= 0
    def isStepsLimitReached = stepsLimit.isDefined && stats.collectSteps >= stepsLimit.get
  }

  class InteractiveExecution[Id](
          graph: Graph[Id, Signal],
          console: ConsoleServer[Id],
          stats: ExecutionStatistics,
          timeLimit: Option[Long],
          stepsLimit: Option[Long],
          globalTerminationCondition: Option[GlobalTerminationCondition[_]]
        ) extends Execution {
    if (console != null) { console.setExecution(this) }
    graph.snapshot
    var converged = false
    var globalTermination = false
    var interval = 0l
    @volatile var steps = 0
    @volatile var userTermination = false
    val lock: AnyRef = new Object()    
    if (globalTerminationCondition.isDefined) {
      interval = globalTerminationCondition.get.aggregationInterval
    }
    val startTime = System.nanoTime
    val nanosecondLimit = timeLimit.getOrElse(0l) * 1000000l

    def step() {
      lock.synchronized {
        steps = 1
        lock.notify
      }
    }
    def continue() {
      lock.synchronized {
        steps = -1
        lock.notifyAll
      }
    }
    def pause() {
      steps = 0
    }
    def reset() { 
      pause
      lock.synchronized {
        graph.reset
        graph.restore
      }
    }
    def terminate() { 
      userTermination = true
      pause
      continue
    }

    def run() {
      lock.synchronized {
        while (!userTermination) { //!converged && !isTimeLimitReached && !isStepsLimitReached && !globalTermination) {
          while (steps == 0) { 
            try { lock.wait } catch { 
              case e: InterruptedException =>
            }
          }
          // signal
          workerApi.signalStep
          awaitIdle
          stats.signalSteps += 1
          while (steps == 0) { 
            try { lock.wait } catch {
              case e: InterruptedException =>
            }
          }
          // collect
          converged = workerApi.collectStep
          stats.collectSteps += 1
          if (steps > 0) { steps -= 1 }
          if (shouldCheckGlobalCondition) {
            globalTermination = isGlobalTerminationConditionMet(globalTerminationCondition.get)
          }
        }
      }

      if (converged) {
        stats.terminationReason = TerminationReason.Converged
      } else if (userTermination) {
        stats.terminationReason = TerminationReason.TerminatedByUser
      } else if (globalTermination) {
        stats.terminationReason = TerminationReason.GlobalConstraintMet
      } else if (isStepsLimitReached) {
        stats.terminationReason = TerminationReason.ComputationStepLimitReached
      } else {
        stats.terminationReason = TerminationReason.TimeLimitReached
      }
      def shouldCheckGlobalCondition = interval > 0 && stats.collectSteps % interval == 0
      def isGlobalTerminationConditionMet[ResultType](gtc: GlobalTerminationCondition[ResultType]): Boolean = {
        val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
        gtc.shouldTerminate(globalAggregateValue)
      }
      def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
      def isTimeLimitReached = timeLimit.isDefined && remainingTimeLimit <= 0
      def isStepsLimitReached = stepsLimit.isDefined && stats.collectSteps >= stepsLimit.get
    }
  }

  protected def optimizedAsynchronousExecution(stats: ExecutionStatistics,
    timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]) = {
    val startTime = System.nanoTime
    workerApi.signalStep
    stats.signalSteps += 1
    val millisecondsSpentAlready = (System.nanoTime - startTime) / 1000000l
    var adjustedTimeLimit: Option[Long] = None
    if (timeLimit.isDefined) {
      adjustedTimeLimit = Some(timeLimit.get - millisecondsSpentAlready)
    }
    pureAsynchronousExecution(stats, adjustedTimeLimit, globalTerminationCondition)
  }

  protected def pureAsynchronousExecution(
    stats: ExecutionStatistics,
    timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]) {
    workerApi.startComputation
    (timeLimit, globalTerminationCondition) match {
      case (None, None) =>
        awaitIdle
      case (Some(limit), None) =>
        val converged = awaitIdle(limit * 1000000l)
        if (converged) {
          stats.terminationReason = TerminationReason.Converged
        } else {
          stats.terminationReason = TerminationReason.TimeLimitReached
        }
      case (None, Some(globalCondition)) =>
        val aggregationOperation = globalCondition.aggregationOperation
        val interval = globalCondition.aggregationInterval * 1000000l
        var converged = false
        while (!converged && !isGlobalTerminationConditionMet(globalCondition)) {
          converged = awaitIdle(interval)
        }
        if (converged) {
          stats.terminationReason = TerminationReason.Converged
        } else {
          stats.terminationReason = TerminationReason.GlobalConstraintMet
        }
        def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition[ValueType]): Boolean = {
          workerApi.pauseComputation
          val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
          workerApi.startComputation
          gtc.shouldTerminate(globalAggregateValue)
        }
      case (Some(limit), Some(globalCondition)) =>
        val aggregationOperation = globalCondition.aggregationOperation
        val timeLimitNanoseconds = limit * 1000000l
        val interval = globalCondition.aggregationInterval * 1000000l
        val startTime = System.nanoTime
        var lastAggregationOperationTime = System.nanoTime - interval
        var converged = false
        var globalTermination = false
        while (!converged && !globalTermination && !isTimeLimitReached) {
          if (intervalHasPassed) {
            globalTermination = isGlobalTerminationConditionMet(globalCondition)
          }
          // waits for whichever remaining time interval/limit is shorter
          converged = awaitIdle(math.min(remainingIntervalTime, remainingTimeLimit))
        }
        if (converged) {
          stats.terminationReason = TerminationReason.Converged
        } else if (globalTermination) {
          stats.terminationReason = TerminationReason.GlobalConstraintMet
        } else {
          stats.terminationReason = TerminationReason.TimeLimitReached
        }
        def intervalHasPassed = remainingIntervalTime <= 0
        def isGlobalTerminationConditionMet[ResultType](gtc: GlobalTerminationCondition[ResultType]): Boolean = {
          workerApi.pauseComputation
          val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
          workerApi.startComputation
          gtc.shouldTerminate(globalAggregateValue)
        }
        def remainingIntervalTime = interval - (System.nanoTime - lastAggregationOperationTime)
        def elapsedTimeNanoseconds = System.nanoTime - startTime
        def remainingTimeLimit = timeLimitNanoseconds - elapsedTimeNanoseconds
        def isTimeLimitReached = remainingTimeLimit <= 0
    }
    workerApi.pauseComputation
  }

  def awaitIdle {
    implicit val timeout = Timeout(Duration.create(1000, TimeUnit.DAYS))
    val resultFuture = coordinatorActor ? OnIdle((c: DefaultCoordinator[_, _], s: ActorRef) => s ! IsIdle(true))
    try {
      val result = Await.result(resultFuture, timeout.duration)
    }
  }

  def awaitIdle(timeoutNanoseconds: Long): Boolean = {
    if (timeoutNanoseconds > 1000000000) {
      implicit val timeout = Timeout(new FiniteDuration(timeoutNanoseconds, TimeUnit.NANOSECONDS))
      val resultFuture = coordinatorActor ? OnIdle((c: DefaultCoordinator[_, _], s: ActorRef) => s ! IsIdle(true))
      try {
        val result = Await.result(resultFuture, timeout.duration)
        true
      } catch {
        case timeout: TimeoutException => false
      }
    } else {
      false
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

  /** WorkerApi */

  def recalculateScores = workerApi.recalculateScores

  def recalculateScoresForVertexWithId(vertexId: Id) = workerApi.recalculateScoresForVertexWithId(vertexId)

  def isIdle = coordinatorProxy.isIdle

  def shutdown = {
    if (config.consoleEnabled) {
      console.shutdown
    }
    workerApi.shutdown
    nodes.par.foreach(_.shutdown)
    system.shutdown
    system.awaitTermination
  }

  def forVertexWithId[VertexType <: Vertex[Id, _], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType = {
    workerApi.forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex[Id, _]) => Unit) = workerApi.foreachVertex(f)

  def aggregate[ResultType](aggregationOperation: ComplexAggregation[_, ResultType]): ResultType = {
    workerApi.aggregateAll(aggregationOperation)
  }

  def setUndeliverableSignalHandler(h: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit) = workerApi.setUndeliverableSignalHandler(h)

  /**
   *  Resets operation statistics and removes all the vertices and edges in this graph.
   *  Leaves the message counters untouched.
   */
  def reset {
    workerApi.reset
  }

  //------------------GraphApi------------------

  /**
   *  Sends `signal` to the vertex with id `vertexId` using the virtual edge id 'edgeId'.
   *  Blocks until the operation has completed if `blocking` is true.
   */
  def sendSignal(signal: Signal, targetId: Id, sourceId: Option[Id], blocking: Boolean) {
    graphEditor.sendSignal(signal, targetId, sourceId, blocking)
  }

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex[Id, _], blocking: Boolean = false) {
    graphEditor.addVertex(vertex, blocking)
  }

  /**
   *  Adds `edge` to the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(sourceVertexId: Id, edge: Edge[Id], blocking: Boolean) {
    graphEditor.addEdge(sourceVertexId, edge, blocking)
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Id, blocking: Boolean = false) {
    graphEditor.removeVertex(vertexId, blocking)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Id], blocking: Boolean = false) {
    graphEditor.removeEdge(edgeId, blocking)
  }

  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None, blocking: Boolean = false) {
    graphEditor.modifyGraph(graphModification, vertexIdHint, blocking)
  }

  private[signalcollect] def sendToWorkerForVertexIdHash(message: Any, vertexIdHash: Int) {
    graphEditor.sendToWorkerForVertexIdHash(message, vertexIdHash)
  }

  /**
   * Creates a snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   * Overwrites any previous snapshot that might exist.
   */
  private[signalcollect] def snapshot = workerApi.snapshot

  /**
   * Restores the last snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   */
  private[signalcollect] def restore = workerApi.restore

  /**
   * Deletes the worker snapshots if they exist.
   */
  private[signalcollect] def deleteSnapshot = workerApi.deleteSnapshot

}
