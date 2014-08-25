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
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.configuration.TerminationReason
import com.signalcollect.console.BreakCondition
import com.signalcollect.console.BreakConditionsAggregator
import com.signalcollect.console.ConsoleServer
import com.signalcollect.console.Execution
import com.signalcollect.coordinator.DefaultCoordinator
import com.signalcollect.coordinator.IsIdle
import com.signalcollect.coordinator.OnIdle
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.EdgeId
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageRecipientRegistry
import com.signalcollect.interfaces.NodeActor
import com.signalcollect.interfaces.SchedulerFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.messaging.DefaultVertexToWorkerMapper
import com.signalcollect.util.AkkaUtil.getActorRefFromSelection
import com.sun.management.OperatingSystemMXBean
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.japi.Creator
import akka.pattern.ask
import akka.util.Timeout
import com.signalcollect.worker.AkkaWorker
import com.signalcollect.interfaces.UndeliverableSignalHandlerFactory
import com.signalcollect.interfaces.EdgeAddedToNonExistentVertexHandlerFactory
import com.signalcollect.interfaces.ExistingVertexHandlerFactory

/**
 * Creator in separate class to prevent excessive closure-capture of the DefaultGraph class (Error[java.io.NotSerializableException DefaultGraph])
 */
case class WorkerCreator[Id: ClassTag, Signal: ClassTag](
  workerId: Int,
  workerFactory: WorkerFactory[Id, Signal],
  numberOfWorkers: Int,
  numberOfNodes: Int,
  messageBusFactory: MessageBusFactory[Id, Signal],
  mapperFactory: MapperFactory[Id],
  storageFactory: StorageFactory[Id, Signal],
  schedulerFactory: SchedulerFactory[Id, Signal],
  existingVertexHandlerFactory: ExistingVertexHandlerFactory[Id, Signal],
  undeliverableSignalHandlerFactory: UndeliverableSignalHandlerFactory[Id, Signal],
  edgeAddedToNonExistentVertexHandlerFactory: EdgeAddedToNonExistentVertexHandlerFactory[Id, Signal],
  heartbeatIntervalInMilliseconds: Int,
  eagerIdleDetection: Boolean,
  throttlingEnabled: Boolean,
  throttlingDuringLoadingEnabled: Boolean,
  supportBlockingGraphModificationsInVertex: Boolean) {
  def create: () => AkkaWorker[Id, Signal] = {
    () =>
      workerFactory.createInstance(
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
        throttlingDuringLoadingEnabled,
        supportBlockingGraphModificationsInVertex)
  }
}

/**
 * Creator in separate class to prevent excessive closure-capture of the DefaultGraph class (Error[java.io.NotSerializableException DefaultGraph])
 */
case class CoordinatorCreator[Id: ClassTag, Signal: ClassTag](
  numberOfWorkers: Int,
  numberOfNodes: Int,
  throttlingEnabled: Boolean,
  messageBusFactory: MessageBusFactory[Id, Signal],
  mapperFactory: MapperFactory[Id],
  heartbeatIntervalInMilliseconds: Long)
  extends Creator[DefaultCoordinator[Id, Signal]] {
  def create: DefaultCoordinator[Id, Signal] = new DefaultCoordinator[Id, Signal](
    numberOfWorkers,
    numberOfNodes,
    throttlingEnabled,
    messageBusFactory,
    mapperFactory,
    heartbeatIntervalInMilliseconds)
}

/**
 * Default graph implementation.
 *
 * Provisions the resources and initializes the workers and the coordinator.
 */
class DefaultGraph[Id: ClassTag, Signal: ClassTag](
  val config: GraphConfiguration[Id, Signal]) extends Graph[Id, Signal] {

  val akkaConfig = AkkaConfig.get(
    config.serializeMessages,
    config.loggingLevel,
    config.kryoRegistrations,
    config.kryoInitializer)
  override def toString: String = "DefaultGraph"

  val system: ActorSystem = {
    config.actorSystem.getOrElse(
      ActorSystemRegistry.retrieve("SignalCollect").
        getOrElse(ActorSystem("SignalCollect", akkaConfig)))
  }

  if (!ActorSystemRegistry.contains(system)) {
    ActorSystemRegistry.register(system)
  }

  val log = system.log

  val console = {
    if (config.consoleEnabled) {
      new ConsoleServer[Id, Signal](config)
    } else {
      null
    }
  }

  log.debug("Provisioning nodes ...")
  val nodeActors = if (config.preallocatedNodes.isDefined) {
    config.preallocatedNodes.get
  } else {
    config.nodeProvisioner.getNodes(system, config.actorNamePrefix, akkaConfig)
  }
  log.debug(s"Received ${nodeActors.length} nodes.")
  // Bootstrap => sent and received messages are not counted for termination detection.
  val bootstrapNodeProxies = nodeActors.map(AkkaProxy.newInstance[NodeActor[Id, Signal]](_)) // MessageBus not initialized at this point.
  val parallelBootstrapNodeProxies = bootstrapNodeProxies.par
  val numberOfNodes = bootstrapNodeProxies.length

  val numberOfWorkers = bootstrapNodeProxies.par.map(_.numberOfCores).sum

  parallelBootstrapNodeProxies foreach (_.initializeMessageBus(numberOfWorkers, numberOfNodes, config.messageBusFactory, config.mapperFactory))

  val mapper = new DefaultVertexToWorkerMapper(numberOfNodes, numberOfWorkers / numberOfNodes)

  val workerActors: Array[ActorRef] = {
    val actors = new Array[ActorRef](numberOfWorkers)
    var workerId = 0
    for (node <- bootstrapNodeProxies) {
      for (core <- 0 until node.numberOfCores) {
        val workerCreator = WorkerCreator[Id, Signal](
          workerId,
          config.workerFactory,
          numberOfWorkers,
          numberOfNodes,
          config.messageBusFactory,
          config.mapperFactory,
          config.storageFactory,
          config.schedulerFactory,
          config.existingVertexHandlerFactory,
          config.undeliverableSignalHandlerFactory,
          config.edgeAddedToNonExistentVertexHandlerFactory,
          config.heartbeatIntervalInMilliseconds,
          config.eagerIdleDetection,
          config.throttlingEnabled,
          config.throttlingDuringLoadingEnabled,
          config.supportBlockingGraphModificationsInVertex)
        val workerName = node.createWorker(workerId, workerCreator.create)
        actors(workerId) = getActorRefFromSelection(system.actorSelection(workerName))
        workerId += 1
      }
    }
    actors
  }

  val coordinatorActor: ActorRef = {
    val coordinatorCreator = CoordinatorCreator[Id, Signal](
      numberOfWorkers,
      numberOfNodes,
      config.throttlingEnabled: Boolean,
      config.messageBusFactory,
      config.mapperFactory,
      config.heartbeatIntervalInMilliseconds)
    system.actorOf(Props(coordinatorCreator.create).withDispatcher("akka.io.pinned-dispatcher"), name = config.actorNamePrefix + "Coordinator")
  }

  if (console != null) { console.setCoordinator(coordinatorActor) }

  // Bootstrap => sent and received messages are not counted for termination detection.
  val bootstrapWorkerProxies = workerActors map (AkkaProxy.newInstance[MessageRecipientRegistry](_)) // MessageBus not initialized at this point.
  val coordinatorProxy = AkkaProxy.newInstance[Coordinator[Id, Signal]](coordinatorActor) // MessageBus not initialized at this point.

  initializeMessageBuses

  parallelBootstrapNodeProxies.foreach(_.initializeIdleDetection)
  lazy val graphEditor = coordinatorProxy.getGraphEditor
  lazy val workerApi = coordinatorProxy.getWorkerApi
  workerApi.initializeIdleDetection

  /** Returns the ConsoleServer */
  def getConsole = console

  def initializeMessageBuses {
    log.debug("Default graph is initializing registries ...")
    val registries: List[MessageRecipientRegistry] = coordinatorProxy :: bootstrapWorkerProxies.toList ++ bootstrapNodeProxies.toList
    for (registry <- registries.par) {
      registry.registerCoordinator(coordinatorActor)
      for (workerId <- (0 until numberOfWorkers).par) {
        registry.registerWorker(workerId, workerActors(workerId))
      }
      for (nodeId <- (0 until numberOfNodes).par) {
        registry.registerNode(nodeId, nodeActors(nodeId))
      }
    }
    log.debug("All registries have been fully initialized.")
  }

  /** GraphApi */

  def execute: ExecutionInformation[Id, Signal] = execute(ExecutionConfiguration)

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

  private[signalcollect] def getWorkerStatistics: List[WorkerStatistics] = {
    workerApi.getIndividualWorkerStatistics
  }

  def execute(parameters: ExecutionConfiguration): ExecutionInformation[Id, Signal] = {
    if (console != null) { console.setExecutionConfiguration(parameters) }
    val executionStartTime = System.nanoTime
    val stats = ExecutionStatistics()
    if (console != null) { console.setExecutionStatistics(stats) }
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
        new InteractiveExecution[Id, Signal](this, console, stats, parameters).run()
        if (console != null) { console.shutdown }
    }
    stats.jvmCpuTime = new FiniteDuration(getJVMCpuTime - jvmCpuStartTime, TimeUnit.NANOSECONDS)
    val executionStopTime = System.nanoTime
    stats.totalExecutionTime = new FiniteDuration(executionStopTime - executionStartTime, TimeUnit.NANOSECONDS)
    val workerStatistics = workerApi.getIndividualWorkerStatistics // TODO: Refactor to use cached values in order to reduce latency.
    ExecutionInformation(config, numberOfWorkers, parameters, stats, workerStatistics.fold(WorkerStatistics())(_ + _), workerStatistics)
  }

  protected def synchronousExecution(
    stats: ExecutionStatistics,
    timeLimit: Option[Long],
    stepsLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition]) {
    var converged = false
    var globalTermination = false
    var interval = 0l
    if (globalTerminationCondition.isDefined) {
      interval = globalTerminationCondition.get.aggregationInterval
    }
    val startTime = System.nanoTime
    val nanosecondLimit = timeLimit.getOrElse(0l) * 1000000l
    log.debug("Starting synchronous execution.")
    while (!converged && !isTimeLimitReached && !isStepsLimitReached && !globalTermination) {
      log.debug(s"Starting signal step #${stats.signalSteps + 1}.")
      workerApi.signalStep
      awaitIdle
      stats.signalSteps += 1
      log.debug(s"Signal step #${stats.signalSteps} completed.")
      log.debug(s"Starting collect step #${stats.collectSteps + 1}.")
      converged = workerApi.collectStep
      stats.collectSteps += 1
      log.debug(s"Collect step #${stats.collectSteps} completed.")
      if (shouldCheckGlobalCondition) {
        log.debug(s"Checking global termination condition.")
        globalTermination = isGlobalTerminationConditionMet(globalTerminationCondition.get)
        log.debug(s"Global termination condition met: $globalTermination.")
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
    def isGlobalTerminationConditionMet[ResultType](gtc: GlobalTerminationCondition): Boolean = {
      val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
      gtc.shouldTerminate(globalAggregateValue)
    }
    def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
    def isTimeLimitReached = timeLimit.isDefined && remainingTimeLimit <= 0
    def isStepsLimitReached = stepsLimit.isDefined && stats.collectSteps >= stepsLimit.get
  }

  /**
   * The interactive execution mode as used by the Console.
   *
   * To use this mode, instantiate it and call its 'run' method, which is the
   * main execution loop for the computation. The instance accepts calls to
   * functions which allow the client to interact with the computation. It
   * supports the following features:
   *  - Resetting the graph
   *  - Performing a partial step (e.g. signal or collect)
   *  - Performing all steps up until before the next signal step
   *  - Continuously running the computation
   *  - Pausing the computation if it is continuously running
   *  - Terminating the computation and Signal/Collect
   *  - Setting and getting multiple break conditions
   *  - GlobalTerminationCondition checking at intervals
   *
   * @constructor create a new InteractiveExecution
   * @param graph the Graph instance
   * @param console the Console instance (may be null)
   * @param stats the ExecutionStatistics instance
   * @param parameters the ExecutionConfiguration instance
   */
  class InteractiveExecution[Id, Signal](
    graph: Graph[Id, Signal],
    console: ConsoleServer[Id, Signal],
    stats: ExecutionStatistics,
    parameters: ExecutionConfiguration) extends Execution {

    var state = "initExecution"
    var iteration = 0
    var converged = false
    var resetting = false
    var globalTermination = false
    @volatile var stepTokens = 0
    @volatile var userTermination = false
    val lock: AnyRef = new Object()
    val infinite = -1

    // Inform the console of our presence if it's enabled
    if (console != null) { console.setExecution(this) }
    else {
      println(
        "Warning: using interactive execution mode without console. To use the console,\n" +
          "         build the graph with: val graph = GraphBuilder.withConsole(true).build")
    }

    // Create an initial snapshot of the graph (to be able to reset later)
    graph.snapshot

    // Set the interval at which to perform global termination condition checks.
    var globalCheckInterval = 0l
    if (parameters.globalTerminationCondition.isDefined) {
      globalCheckInterval =
        parameters.globalTerminationCondition.get.aggregationInterval
    }

    /**
     * Return true if the global condition is met.
     *
     * @param gtc the GlobalTerminationCondition to check
     */
    private def isGlobalTerminationConditionMet[ResultType](
      gtc: GlobalTerminationCondition): Boolean = {
      val globalAggregateValue = workerApi.aggregateAll(gtc.aggregationOperation)
      gtc.shouldTerminate(globalAggregateValue)
    }

    /** Returns true if a global condition check should be performed */
    private def shouldCheckGlobalCondition = (globalCheckInterval > 0 &&
      stats.collectSteps % globalCheckInterval == 0)

    // Store a map of break conditions and a map of reached break conditions.
    // These are checked after each signal and collect step.
    var conditions = Map[String, BreakCondition]()
    var conditionsReached = Map[String, String]()
    var conditionCounter = 0

    /** Add a BreakCondition. */
    def addCondition(condition: BreakCondition) {
      conditionCounter += 1
      conditions += (conditionCounter.toString -> condition)
    }

    /** Remove a BreakCondition. */
    def removeCondition(id: String) {
      conditions -= id
    }

    /** Perform a single, partial step. */
    def step {
      lock.synchronized {
        stepTokens = 1
        lock.notifyAll
      }
    }

    /** Complete the current iteration and pause before the next signal step. */
    def collect {
      lock.synchronized {
        stepTokens = state match {
          case "pausedBeforeSignal" => 4
          case "pausedBeforeChecksAfterSignal" => 3
          case "pausedBeforeCollect" => 2
          case "pausedBeforeChecksAfterCollect" => 1
          case "pausedBeforeGlobalChecks" => 0
        }
        if (shouldCheckGlobalCondition) { stepTokens += 1 }
        lock.notifyAll
      }
    }

    /** Continue computation until paused by the user or a break condition. */
    def continue {
      lock.synchronized {
        stepTokens = infinite
        lock.notifyAll
      }
    }

    /** Pause the computation. */
    def pause {
      stepTokens = 0
    }

    /** Reset the graph to its initial state and pause the computation. */
    def reset {
      pause
      lock.synchronized {
        setState("resetting")
        resetting = true
        conditionsReached = Map[String, String]()
        converged = false
        stepTokens = 0
        iteration = 0
        graph.reset
        graph.restore
        lock.notifyAll
      }
    }

    /** Terminate the computation and Signal/Collect. */
    def terminate {
      userTermination = true
      setState("terminating")
      resetting = true
      pause
      continue
    }

    /**
     * Set current state and if the Console is enabled, inform all clients.
     *
     * @param s the new state
     */
    private def setState(s: String) {
      state = s
      if (console != null) { console.sockets.updateClientState() }
    }

    /**
     * Do nothing until there are new stepTokens or the computation is reset.
     *
     * @param s the state to be published to clients
     */
    private def waitAs(s: String) {
      while (stepTokens == 0 && !resetting) {
        setState(s)
        try { lock.wait } catch {
          case e: InterruptedException =>
        }
      }
    }

    /**
     * Publish the state s and perform function f, using one stepToken.
     *
     * @param s the state to be published to clients
     * @param f the function to perform
     */
    private def performStep(s: String, f: () => Unit) {
      if (!resetting) {
        setState(s)
        if (stepTokens > 0) { stepTokens -= 1 }
        f()
      }
    }

    /**
     * Check user break conditions and pause if one of them was reached.
     *
     * The current state is passed along to the BreakConditionsAggregator
     * because not all conditions make sense depending on the type. See the
     * BreakConditionsAggregator documentation.
     */
    private def checkBreakConditions() {
      conditionsReached = workerApi.aggregateAll(
        new BreakConditionsAggregator(conditions, state))
      if (conditionsReached.size > 0) {
        stepTokens = 0
        setState("pausing")
      }
    }

    /**
     * Start the main execution loop.
     *
     * The loop consists of five phases: 1) Signalling, 2) checks after
     * signalling, 3) collecting, 4) checks after collecting and 5) global
     * termination condition checks. Each phase decrements the stepToken
     * counter by 1. If stepTokens is 0, the computation is paused. The
     * counter is increased by 1 when performing a partial step, and by the
     * required amount to complete the iteration whe performing a full step.
     * When continuing the computation, stepTokens is set to infinite, which
     * is simply an alias for -1.
     */
    def run() {
      println("Entering interactive execution mode")
      lock.synchronized {
        while (!userTermination) {
          iteration += 1

          // Signalling
          waitAs("pausedBeforeSignal")
          performStep("signalling", () => {
            workerApi.signalStep
            awaitIdle
            stats.signalSteps += 1
          })

          // Checks after signalling
          waitAs("pausedBeforeChecksAfterSignal")
          performStep("checksAfterSignal", checkBreakConditions)

          // Collecting
          waitAs("pausedBeforeCollect")
          performStep("collecting", () => {
            converged = workerApi.collectStep
            stats.collectSteps += 1
            if (converged) {
              stepTokens = 0
              waitAs("converged")
            }
          })

          // Checks after collecting
          waitAs("pausedBeforeChecksAfterCollect")
          performStep("checksAfterCollect", checkBreakConditions)

          // Global condition checks
          if (shouldCheckGlobalCondition) {
            waitAs("pausedBeforeGlobalChecks")
            performStep("globalChecks", () => {
              globalTermination = isGlobalTerminationConditionMet(
                parameters.globalTerminationCondition.get)
              if (globalTermination) {
                stepTokens = 0
                waitAs("globalConditionReached")
              }
            })
          }

          resetting = false
        }
      }

      // Record the termination reason
      if (converged) {
        stats.terminationReason = TerminationReason.Converged
      } else if (userTermination) {
        stats.terminationReason = TerminationReason.TerminatedByUser
      }

      println("Exiting interactive execution mode")
    }
  }

  protected def optimizedAsynchronousExecution(
    stats: ExecutionStatistics,
    timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition]) = {
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
    globalTerminationCondition: Option[GlobalTerminationCondition]) {
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
        var lastAggregationOperationTime = System.nanoTime - interval
        var converged = false
        var globalTermination = false
        while (!converged && !isGlobalTerminationConditionMet(globalCondition)) {
          if (intervalHasPassed) {
            lastAggregationOperationTime = System.nanoTime
            globalTermination = isGlobalTerminationConditionMet(globalCondition)
          }
          // waits for whichever remaining time interval/limit is shorter
          converged = awaitIdle(remainingIntervalTime)
        }
        if (converged) {
          stats.terminationReason = TerminationReason.Converged
        } else {
          stats.terminationReason = TerminationReason.GlobalConstraintMet
        }
        def intervalHasPassed = remainingIntervalTime <= 0
        def remainingIntervalTime = interval - (System.nanoTime - lastAggregationOperationTime)
        def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition): Boolean = {
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
            lastAggregationOperationTime = System.nanoTime
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
        def isGlobalTerminationConditionMet[ResultType](gtc: GlobalTerminationCondition): Boolean = {
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
    awaitIdle(Duration.create(1, TimeUnit.DAYS).toNanos)
  }

  def awaitIdle(timeoutNanoseconds: Long): Boolean = {
    if (timeoutNanoseconds > 1000000000) {
      implicit val timeout = Timeout(new FiniteDuration(timeoutNanoseconds, TimeUnit.NANOSECONDS))
      // Add a new "on idle" action to the coordinator actors. The action is to send a message back to ourselves.
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

  def recalculateScoresForVertexWithId(vertexId: Id) {
    workerApi.recalculateScoresForVertexWithId(vertexId)
  }

  def isIdle = coordinatorProxy.isIdle

  def shutdown {
    if (console != null) { console.shutdown }
    // Only shut down the actor system if we created it.
    if (config.actorSystem.isEmpty) {
      // The node proxies also shutdown their respective actor systems.
      parallelBootstrapNodeProxies.foreach(_.shutdown)
      system.shutdown
      system.awaitTermination
      ActorSystemRegistry.remove(system)
    } else {
      // If the system is preserved, just cleanup the actors.
      workerActors.foreach(_ ! PoisonPill)
      nodeActors.foreach(_ ! PoisonPill)
      coordinatorActor ! PoisonPill
    }
  }

  def forVertexWithId[VertexType <: Vertex[Id, _, Id, Signal], ResultType](
    vertexId: Id, f: VertexType => ResultType): ResultType = {
    workerApi.forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex[Id, _, Id, Signal]) => Unit) {
    workerApi.foreachVertex(f)
  }

  def foreachVertexWithGraphEditor(
    f: GraphEditor[Id, Signal] => Vertex[Id, _, Id, Signal] => Unit) {
    workerApi.foreachVertexWithGraphEditor(f)
  }

  def aggregate[ResultType](
    aggregationOperation: ComplexAggregation[_, ResultType]): ResultType = {
    workerApi.aggregateAll(aggregationOperation)
  }

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
    graphEditor.flush
  }

  /**
   *  Adds `vertex` to the graph.
   *
   *  @note If a vertex with the same id already exists, then this operation will be ignored and NO warning is logged.
   */
  def addVertex(vertex: Vertex[Id, _, Id, Signal], blocking: Boolean = false) {
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

  /**
   *  Loads a graph using the provided `graphModification` function.
   *  Blocks until the operation has completed if `blocking` is true.
   *
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None, blocking: Boolean = false) {
    graphEditor.modifyGraph(graphModification, vertexIdHint, blocking)
  }

  /**
   *  Loads a graph using the provided iterator of `graphModification` functions.
   *
   *  @note Does not block.
   *  @note The vertexIdHint can be used to supply a characteristic vertex ID to give a hint to the system on which worker
   *        the loading function will be able to exploit locality.
   *  @note For distributed graph loading use separate calls of this method with vertexIdHints targeting different workers.
   */
  def loadGraph(graphModifications: Iterator[GraphEditor[Id, Signal] => Unit], vertexIdHint: Option[Id]) {
    graphEditor.loadGraph(graphModifications, vertexIdHint)
  }

  private[signalcollect] def sendToWorkerForVertexIdHash(message: Any, vertexIdHash: Int) {
    graphEditor.sendToWorkerForVertexIdHash(message, vertexIdHash)
  }

  private[signalcollect] def flush {
    graphEditor.flush
  }

  private[signalcollect] def sendToActor(actor: ActorRef, message: Any) {
    graphEditor.sendToActor(actor, message)
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
