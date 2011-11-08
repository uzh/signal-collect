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

package com.signalcollect.implementations.coordinator

import com.signalcollect.configuration._
import com.signalcollect.implementations.messaging.AbstractMessageRecipient
import com.signalcollect.interfaces._
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.ExecutionInformation
import com.signalcollect.SynchronousExecutionMode
import com.signalcollect.OptimizedAsynchronousExecutionMode
import com.signalcollect.PureAsynchronousExecutionMode
import com.signalcollect.ExecutionStatistics
import com.signalcollect.ContinuousAsynchronousExecution
import com.signalcollect.TerminationReason
import com.signalcollect.Converged
import com.signalcollect.GlobalTerminationCondition
import com.signalcollect.TimeLimitReached
import com.signalcollect.GlobalConstraintMet
import com.signalcollect.ComputationStepLimitReached

class Coordinator(protected val workerApi: WorkerApi, config: GraphConfiguration) {

  def execute(parameters: ExecutionConfiguration): ExecutionInformation = {
    val executionStartTime = System.nanoTime

    val graphIdleWaitingStart = System.nanoTime
    
    workerApi.signalSteps = 0
    workerApi.collectSteps = 0

    workerApi.setSignalThreshold(parameters.signalThreshold)
    workerApi.setCollectThreshold(parameters.collectThreshold)

    workerApi.info("Waiting for graph loading to finish ...")

    val loadingDone = workerApi.awaitIdle()
    
    val graphIdleWaitingStop = System.nanoTime
    val graphIdleWaitingTime = graphIdleWaitingStop - graphIdleWaitingStart

    workerApi.info("Running garbage collection before execution ...")
    
    val gcStartTime = System.nanoTime
    System.gc // collect garbage before execution 
    val gcEndTime = System.nanoTime
    val preExecutionGcTime = gcEndTime - gcStartTime

    workerApi.info("Starting computation ...")
    val jvmCpuStartTime = getJVMCpuTime
    val startTime = System.nanoTime

    /*******************************/

    var terminationReason: TerminationReason = Converged // default reason

    parameters.executionMode match {
      case SynchronousExecutionMode => terminationReason = synchronousExecution(parameters.timeLimit, parameters.stepsLimit, parameters.globalTerminationCondition)
      case OptimizedAsynchronousExecutionMode => terminationReason = optimizedAsynchronousExecution(parameters.timeLimit, parameters.globalTerminationCondition)
      case PureAsynchronousExecutionMode => terminationReason = pureAsynchronousExecution(parameters.timeLimit, parameters.globalTerminationCondition)
      case ContinuousAsynchronousExecution => continuousAsynchronousExecution
    }

    /*******************************/

    val stopTime = System.nanoTime
    val jvmCpuStopTime = getJVMCpuTime
    val totalTime = stopTime - startTime
    val totalJvmCpuTime = jvmCpuStopTime - jvmCpuStartTime

    workerApi.info("Done.")

    val workerStatistics = workerApi.getWorkerStatistics
    val aggregatedWorkerStatistics = workerStatistics.fold(WorkerStatistics())(_ + _)

    val executionFinishTime = System.nanoTime
    val totalExecutionTime = executionFinishTime - executionStartTime
    
    val executionStatistics = ExecutionStatistics(
      signalSteps = workerApi.signalSteps,
      collectSteps = workerApi.collectSteps,
      computationTimeInMilliseconds = totalTime / 1000000l,
      totalExecutionTimeInMilliseconds = totalExecutionTime / 1000000l,
      jvmCpuTimeInMilliseconds = totalJvmCpuTime / 1000000l,
      graphIdleWaitingTimeInMilliseconds = graphIdleWaitingTime / 1000000l,
      preExecutionGcTimeInMilliseconds = preExecutionGcTime / 1000000l,
      terminationReason = terminationReason)

    val stats = ExecutionInformation(
      config,
      parameters,
      executionStatistics,
      aggregatedWorkerStatistics,
      workerStatistics)
    workerApi.info(stats)
    
    stats
  }

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }

  protected def optimizedAsynchronousExecution(timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]): TerminationReason = {
    val startTime = System.nanoTime
    workerApi.signalStep
    val millisecondsSpentAlready = (System.nanoTime - startTime) / 1000000l
    var adjustedTimeLimit: Option[Long] = None
    if (timeLimit.isDefined) {
      adjustedTimeLimit = Some(timeLimit.get - millisecondsSpentAlready)
    }
    pureAsynchronousExecution(adjustedTimeLimit, globalTerminationCondition)
  }

  protected def pureAsynchronousExecution(timeLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]): TerminationReason = {
    workerApi.startComputation
    var terminationReason: TerminationReason = Converged
    (timeLimit, globalTerminationCondition) match {
      case (None, None) =>
        workerApi.awaitIdle()
      case (Some(limit), None) =>
        val converged = workerApi.awaitIdle(limit * 1000000l)
        if (!converged) {
          terminationReason = TimeLimitReached
        }
      case (None, Some(globalCondition)) =>
        val aggregationOperation = globalCondition.aggregationOperation
        val interval = globalCondition.aggregationInterval * 1000000l
        var converged = false
        var globalTermination = false
        while (!converged && !globalTermination) {
          converged = workerApi.awaitIdle(interval)
          if (!converged) {
            globalTermination = isGlobalTerminationConditionMet(globalCondition)
          }
        }
        if (!converged) {
          terminationReason = GlobalConstraintMet
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
          converged = workerApi.awaitIdle(math.min(remainingIntervalTime, remainingTimeLimit))
        }
        if (timeLimitReached) {
          terminationReason = TimeLimitReached
        } else if (globalTermination) {
          terminationReason = GlobalConstraintMet
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
    terminationReason
  }

  protected def continuousAsynchronousExecution {
    workerApi.startComputation
  }

  protected def synchronousExecution(
    timeLimit: Option[Long],
    stepsLimit: Option[Long],
    globalTerminationCondition: Option[GlobalTerminationCondition[_]]): TerminationReason = {
    var terminationReason: TerminationReason = Converged
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
      converged = workerApi.collectStep
      if (shouldCheckGlobalCondition) {
        globalTermination = isGlobalTerminationConditionMet(globalTerminationCondition.get)
      }
    }
    if (isTimeLimitReached) {
      terminationReason = TimeLimitReached
    } else if (isStepsLimitReached) {
      terminationReason = ComputationStepLimitReached
    } else if (globalTermination) {
      terminationReason = GlobalConstraintMet
    }
    def shouldCheckGlobalCondition = interval > 0 && workerApi.collectSteps % interval == 0
    def isGlobalTerminationConditionMet[ValueType](gtc: GlobalTerminationCondition[ValueType]): Boolean = {

      val globalAggregateValue = workerApi.aggregate(gtc.aggregationOperation)
      gtc.shouldTerminate(globalAggregateValue)
    }
    def remainingTimeLimit = nanosecondLimit - (System.nanoTime - startTime)
    def isTimeLimitReached = timeLimit.isDefined && remainingTimeLimit <= 0
    def isStepsLimitReached = stepsLimit.isDefined && workerApi.collectSteps >= stepsLimit.get

    terminationReason
  }

}