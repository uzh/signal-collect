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

package com.signalcollect.implementations.graph

import com.signalcollect.interfaces._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.configuration._
import com.signalcollect._
import akka.actor.ActorRef
import com.signalcollect.implementations.messaging.DefaultVertexToWorkerMapper
import akka.actor.ActorSystem
import com.signalcollect.implementations.logging.DefaultLogger
import akka.actor.Props
import com.signalcollect.implementations.worker.AkkaWorker
import com.signalcollect.implementations.messaging.AkkaProxy
import akka.actor.ReceiveTimeout
import java.util.concurrent.TimeUnit
import akka.util.duration._
import akka.util.Duration
import akka.util.FiniteDuration
import configuration.TerminationReason
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import akka.util.duration._
import akka.dispatch.Await
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import com.signalcollect.interfaces.LogMessage
import scala.util.Random

/**
 * Default graph implementation.
 *
 * Provisions the resources and initializes the workers and the coordinator.
 */
class DefaultGraph(val config: GraphConfiguration = GraphConfiguration()) extends Graph {

  val mapper = new DefaultVertexToWorkerMapper(config.numberOfWorkers)  
  
  val system = ActorSystem("SignalCollect", ConfigFactory.parseString(AkkaConfig.getConfig))

  val workerActors: Array[ActorRef] = {
    val actors = new Array[ActorRef](config.numberOfWorkers)
    for (workerId <- 0 until config.numberOfWorkers) {
      val workerName = "Worker" + workerId
      config.akkaDispatcher match {
        case EventBased => actors(workerId) = system.actorOf(Props(config.workerFactory.createInstance(workerId, config)), name = workerName)
//        case Pinned => actors(workerId) = system.actorOf(Props(config.workerFactory.createInstance(workerId, config)).withDispatcher("pinned-dispatcher"), name = workerName)
        case Pinned => actors(workerId) = system.actorOf(Props().withCreator(config.workerFactory.createInstance(workerId, config)).withDispatcher("akka.actor.pinned-dispatcher"), name = workerName)
      }
    }
    actors
  }

  val coordinatorActor: ActorRef = {
    val numberOfRegistries = config.numberOfWorkers // see initializeMessageBuses, coordinator is not counting proxy messages, so it does not have to be counted here
    val registrationMessagesPerRegistry = config.numberOfWorkers + 2 // +2 for coordinator and logger (the logger does not have its own registry, because it only receives messages)
    val initializationMessagesSent = numberOfRegistries * registrationMessagesPerRegistry
    config.akkaDispatcher match {
        case EventBased => system.actorOf(Props(new DefaultCoordinator(config)), name = "Coordinator")
        case Pinned => system.actorOf(Props().withCreator(new DefaultCoordinator(config)), name = "Coordinator") //.withDispatcher("akka.actor.pinned-dispatcher")
    }
  }

  val loggerActor: ActorRef = {
    system.actorOf(Props(new DefaultLogger(config.logger)), name = "Logger")
  }

  val workerProxies = workerActors map (AkkaProxy.newInstance[Worker](_, loggerActor))
  val coordinatorProxy = AkkaProxy.newInstance[Coordinator](coordinatorActor, loggerActor)

  initializeMessageBuses
    
  awaitIdle

  def initializeMessageBuses {
    // the MessageBus registries
    val registries: List[MessageRecipientRegistry] = coordinatorProxy :: workerProxies.toList
    for (registry <- registries.par) {
      try {
        registry.registerCoordinator(coordinatorActor)
      } catch {
        case e: Exception => loggerActor ! Severe("Exception in `initializeMessageBuses`:" + e.getCause + "\n" + e, this.toString)
      }
      for (workerId <- (0 until config.numberOfWorkers).par) {
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
    }
    stats.jvmCpuTime = new FiniteDuration(getJVMCpuTime - jvmCpuStartTime, TimeUnit.NANOSECONDS)
    val executionStopTime = System.nanoTime
    stats.totalExecutionTime = new FiniteDuration(executionStopTime - executionStartTime, TimeUnit.NANOSECONDS)
    val workerStatistics = workerApi.getIndividualWorkerStatistics
    ExecutionInformation(config, parameters, stats, workerStatistics.fold(WorkerStatistics())(_ + _), workerStatistics)
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
      stats.signalSteps += 1
      converged = workerApi.collectStep
      stats.collectSteps += 1
      if (shouldCheckGlobalCondition) {
        globalTermination = isGlobalTerminationConditionMet(globalTerminationCondition.get)
      }
    }
    if (isTimeLimitReached) {
      stats.terminationReason = TerminationReason.TimeLimitReached
    } else if (isStepsLimitReached) {
      stats.terminationReason = TerminationReason.ComputationStepLimitReached
    } else if (globalTermination) {
      stats.terminationReason = TerminationReason.GlobalConstraintMet
    }
    def shouldCheckGlobalCondition = interval > 0 && stats.collectSteps % interval == 0
    def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition[ValueType]): Boolean = {
      val globalAggregateValue = workerApi.aggregate(gtc.aggregationOperation)
      gtc.shouldTerminate(globalAggregateValue)
    }
    def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
    def isTimeLimitReached = timeLimit.isDefined && remainingTimeLimit <= 0
    def isStepsLimitReached = stepsLimit.isDefined && stats.collectSteps >= stepsLimit.get
  }

  protected def optimizedAsynchronousExecution(stats: ExecutionStatistics,
      timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]) = {
    val startTime = System.nanoTime
    workerApi.signalStep
    awaitIdle
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
    stats.terminationReason = TerminationReason.Converged
    (timeLimit, globalTerminationCondition) match {
      case (None, None) =>
        awaitIdle
      case (Some(limit), None) =>
        val converged = awaitIdle(limit * 1000000l)
        if (!converged) {
          stats.terminationReason = TerminationReason.TimeLimitReached
        }
      case (None, Some(globalCondition)) =>
        val aggregationOperation = globalCondition.aggregationOperation
        val interval = globalCondition.aggregationInterval * 1000000l
        var converged = false
        var globalTermination = false
        while (!converged && !globalTermination) {
          converged = awaitIdle(interval)
          if (!converged) {
            globalTermination = isGlobalTerminationConditionMet(globalCondition)
          }
        }
        if (!converged) {
          stats.terminationReason = TerminationReason.GlobalConstraintMet
        }
        def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition[ValueType]): Boolean = {
          workerApi.pauseComputation
          val globalAggregateValue = workerApi.aggregate(gtc.aggregationOperation)
          workerApi.startComputation
          gtc.shouldTerminate(globalAggregateValue)
        }
      case (Some(limit), Some(globalCondition)) =>
        val aggregationOperation = globalCondition.aggregationOperation
        val nanosecondLimit = limit * 1000000l
        val interval = globalCondition.aggregationInterval * 1000000l
        val startTime = System.nanoTime
        var lastAggregationOperationTime = System.nanoTime - interval
        var converged = false
        var globalTermination = false
        var timeLimitReached = false
        while (!converged && !globalTermination && !isTimeLimitReached) {
          if (intervalHasPassed) {
            globalTermination = isGlobalTerminationConditionMet(globalCondition)
          }
          // waits for whichever remaining time interval/limit is shorter
          converged = awaitIdle(math.min(remainingIntervalTime, remainingTimeLimit))
        }
        if (timeLimitReached) {
          stats.terminationReason = TerminationReason.TimeLimitReached
        } else if (globalTermination) {
          stats.terminationReason = TerminationReason.GlobalConstraintMet
        }
        def intervalHasPassed = remainingIntervalTime <= 0
        def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition[ValueType]): Boolean = {
          workerApi.pauseComputation
          val globalAggregateValue = workerApi.aggregate(gtc.aggregationOperation)
          workerApi.startComputation
          gtc.shouldTerminate(globalAggregateValue)
        }
        def remainingIntervalTime = interval - (System.nanoTime - lastAggregationOperationTime)
        def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
        def isTimeLimitReached = remainingTimeLimit <= 0
    }
    workerApi.pauseComputation
  }  

  def awaitIdle {
    implicit val timeout = Timeout(1000 days)
    val resultFuture = coordinatorActor ? OnIdle((c: DefaultCoordinator, s: ActorRef) => s ! IsIdle(true))
    try {
      val result = Await.result(resultFuture, timeout.duration)
    }
  }
  
  def awaitIdle(timeoutNanoseconds: Long): Boolean = {
    implicit val timeout = Timeout(new FiniteDuration(timeoutNanoseconds, TimeUnit.NANOSECONDS))
    val resultFuture = coordinatorActor ? OnIdle((c: DefaultCoordinator, s: ActorRef) => s ! IsIdle(true))
    try {
      val result = Await.result(resultFuture, timeout.duration + { 10 seconds })
      true
    } catch {
      case e: AskTimeoutException => false
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

  def recalculateScoresForVertexWithId(vertexId: Any) = workerApi.recalculateScoresForVertexWithId(vertexId)

  def isIdle = coordinatorProxy.isIdle

  def shutdown = {
    workerApi.shutdown
    system.shutdown
  }

  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    workerApi.forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex) => Unit) = workerApi.foreachVertex(f)

  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]): ValueType = {
    workerApi.aggregate(aggregationOperation)
  }

  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphEditor) => Unit) = workerApi.setUndeliverableSignalHandler(h)

  //------------------GraphApi------------------

  /**
   *  Sends `signal` along the edge with id `edgeId`.
   */
  def sendSignalAlongEdge(signal: Any, edgeId: EdgeId[Any, Any], blocking: Boolean = false) {
    graphEditor.sendSignalAlongEdge(signal, edgeId, blocking)
  }

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex, blocking: Boolean = false) {
    graphEditor.addVertex(vertex, blocking)
  }

  /**
   *  Adds `edge` to the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If an edge with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addEdge(edge: Edge, blocking: Boolean = false) {
    graphEditor.addEdge(edge, blocking)
  }

  /**
   *  Adds edges to vertices that satisfy `sourceVertexPredicate`. The edges added are created by `edgeFactory`,
   *  which will receive the respective vertex as a parameter.
   */
  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge, blocking: Boolean = false) {
    graphEditor.addPatternEdge(sourceVertexPredicate, edgeFactory, blocking)
  }

  /**
   *  Removes the vertex with id `vertexId` from the graph.
   *
   *  @note If no vertex with this id is found, then the operation is ignored and a warning is logged.
   */
  def removeVertex(vertexId: Any, blocking: Boolean = false) {
    graphEditor.removeVertex(vertexId, blocking)
  }

  /**
   *  Removes the edge with id `edgeId` from the graph.
   *
   *  @note If no vertex with the required source id is found, then the operation is ignored and a warning is logged.
   *  @note If no edge with with this id is found, then this operation will be ignored and a warning is logged.
   */
  def removeEdge(edgeId: EdgeId[Any, Any], blocking: Boolean = false) {
    graphEditor.removeEdge(edgeId, blocking)
  }

  /**
   *  Removes all vertices that satisfy the `shouldRemove` predicate from the graph.
   */
  def removeVertices(shouldRemove: Vertex => Boolean, blocking: Boolean = false) {
    graphEditor.removeVertices(shouldRemove, blocking)
  }
  
  def loadGraph(vertexIdHint: Option[Any] = None, graphLoader: GraphEditor => Unit, blocking: Boolean = false) {
    graphEditor.loadGraph(vertexIdHint, graphLoader, blocking)
  }

}