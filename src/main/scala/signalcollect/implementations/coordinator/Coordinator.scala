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

import signalcollect.configuration._
import signalcollect.implementations.messaging.AbstractMessageRecipient
import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.api.Factory
import signalcollect.interfaces._
import java.util.concurrent.ArrayBlockingQueue
import signalcollect.interfaces.ComputeGraph
import java.util.concurrent.BlockingQueue
import java.lang.management._
import com.sun.management.OperatingSystemMXBean
import signalcollect.api._

class Coordinator(protected val workerApi: WorkerApi, config: Configuration) {

  def execute(parameters: ExecutionConfiguration): ExecutionInformation = {
    workerApi.signalSteps = 0
    workerApi.collectSteps = 0

    workerApi.setSignalThreshold(parameters.signalThreshold)
    workerApi.setCollectThreshold(parameters.collectThreshold)

    workerApi.logCoordinatorMessage("Waiting for graph loading to finish ...")

    val graphLoadingWait = workerApi.awaitIdle

    workerApi.logCoordinatorMessage("Starting computation ...")
    val jvmCpuStartTime = getJVMCpuTime
    val startTime = System.nanoTime

    /*******************************/

    config.executionConfiguration.executionMode match {
      case SynchronousExecutionMode => synchronousExecution(config.executionConfiguration.stepsLimit)
      case OptimizedAsynchronousExecutionMode => optimizedAsynchronousExecution
      case PureAsynchronousExecutionMode => pureAsynchronousExecution
    }

    /*******************************/

    val stopTime = System.nanoTime
    val jvmCpuStopTime = getJVMCpuTime
    val totalTime: Long = stopTime - startTime
    val totalJvmCpuTime: Long = jvmCpuStopTime - jvmCpuStartTime

    workerApi.logCoordinatorMessage("\t\t\tDONE")

    val workerStatistics = workerApi.getWorkerStatistics
    val aggregatedWorkerStatistics = workerStatistics.fold(WorkerStatistics())(_ + _)

    val executionStatistics = ExecutionStatistics(
      signalSteps = workerApi.signalSteps,
      collectSteps = workerApi.collectSteps,
      computationTimeInMilliseconds = (totalTime / 1000000.0).toLong,
      jvmCpuTimeInMilliseconds = (totalJvmCpuTime / 1000000.0).toLong,
      graphLoadingWaitInMilliseconds = (graphLoadingWait / 1000000.0).toLong)

    ExecutionInformation(
      config,
      parameters,
      executionStatistics,
      aggregatedWorkerStatistics,
      workerStatistics)
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