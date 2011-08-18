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

class Coordinator(protected val workerApi: WorkerApi, config: Configuration) {

  def execute(parameters: ExecutionConfiguration): ExecutionInformation = {   
    workerApi.signalSteps = 0
    workerApi.collectSteps = 0

    workerApi.setSignalThreshold(parameters.signalThreshold)
    workerApi.setCollectThreshold(parameters.collectThreshold)

    workerApi.info("Waiting for graph loading to finish ...")

    val graphLoadingWait = workerApi.awaitIdle

    workerApi.info("Running garbage collection before execution ...")
    
    val gcStartTime = System.nanoTime
    System.gc // collect garbage before execution 
    val gcEndTime = System.nanoTime
    val preExecutionGcTime = gcEndTime - gcStartTime
    
    
    workerApi.info("Starting computation ...")
    val jvmCpuStartTime = getJVMCpuTime
    val startTime = System.nanoTime

    /*******************************/

    parameters.executionMode match {
      case SynchronousExecutionMode => synchronousExecution(parameters.stepsLimit)
      case OptimizedAsynchronousExecutionMode => optimizedAsynchronousExecution
      case PureAsynchronousExecutionMode => pureAsynchronousExecution
    }

    /*******************************/

    val stopTime = System.nanoTime
    val jvmCpuStopTime = getJVMCpuTime
    val totalTime: Long = stopTime - startTime
    val totalJvmCpuTime: Long = jvmCpuStopTime - jvmCpuStartTime

    workerApi.info("Done.")

    val workerStatistics = workerApi.getWorkerStatistics
    val aggregatedWorkerStatistics = workerStatistics.fold(WorkerStatistics())(_ + _)

    val executionStatistics = ExecutionStatistics(
      signalSteps = workerApi.signalSteps,
      collectSteps = workerApi.collectSteps,
      computationTimeInMilliseconds = (totalTime / 1000000.0).toLong,
      jvmCpuTimeInMilliseconds = (totalJvmCpuTime / 1000000.0).toLong,
      graphLoadingWaitInMilliseconds = (graphLoadingWait / 1000000.0).toLong,
      preExecutionGcTimeInMilliseconds = (preExecutionGcTime / 1000000.0).toLong)

    val stats = ExecutionInformation(
      config,
      parameters,
      executionStatistics,
      aggregatedWorkerStatistics,
      workerStatistics)
    workerApi.info(stats)
    stats
  }

  //protected def performComputation(parameters: ExecutionParameters)

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }

  protected def optimizedAsynchronousExecution {
    workerApi.signalStep
    pureAsynchronousExecution
  }

  protected def pureAsynchronousExecution {
    workerApi.startComputation
    workerApi.awaitIdle
    workerApi.pauseComputation
  }

  protected def synchronousExecution(stepsLimit: Option[Long]) {
    var done = false
    while (!done && (!stepsLimit.isDefined || workerApi.collectSteps < stepsLimit.get)) {
      workerApi.signalStep
      done = workerApi.collectStep
    }
  }

}