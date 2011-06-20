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
import signalcollect.api.Factory
import signalcollect.api.Factory._
import signalcollect.interfaces._
import java.util.concurrent.ArrayBlockingQueue
import signalcollect.interfaces.ComputeGraph
import java.util.concurrent.BlockingQueue
import java.lang.management._
import com.sun.management.OperatingSystemMXBean

class AsynchronousCoordinator(workerApi: WorkerApi, configurationMap: Map[String, Any])
  extends Coordinator(workerApi, configurationMap) with AsynchronousExecution

class SynchronousCoordinator(workerApi: WorkerApi, configurationMap: Map[String, Any])
  extends Coordinator(workerApi, configurationMap) with SynchronousExecution

abstract class Coordinator(protected val workerApi: WorkerApi, var statsMap: Map[String, Any]) {

  protected var stepsLimit = Int.MaxValue

  def setStepsLimit(l: Int) {
    stepsLimit = l
  }

  def execute: ComputationStatistics = {
    workerApi.signalSteps = 0
    workerApi.collectSteps = 0

    workerApi.logCoordinatorMessage("Waiting for graph loading to finish ...")

    val graphLoadingWait = workerApi.awaitIdle

    workerApi.logCoordinatorMessage("Starting computation ...")
    val jvmCpuStartTime = getJVMCpuTime
    val startTime = System.nanoTime

    /*******************************/
    performComputation
    /*******************************/

    val stopTime = System.nanoTime
    val jvmCpuStopTime = getJVMCpuTime
    val totalTime: Long = stopTime - startTime
    val totalJvmCpuTime: Long = jvmCpuStopTime - jvmCpuStartTime
    workerApi.logCoordinatorMessage("\t\t\tDONE")

    statsMap += (("stepsLimit", stepsLimit))
    statsMap += (("signalCollectSteps", workerApi.collectSteps))

    val workerStats = workerApi.getWorkerStats

    statsMap += (("vertexCollectOperations", workerStats.collectOperationsExecuted))
    statsMap += (("vertexSignalOperations", workerStats.signalOperationsExecuted))

    statsMap += (("numberOfVertices", workerStats.verticesAdded - workerStats.verticesRemoved))
    statsMap += (("verticesAdded", workerStats.verticesAdded))
    statsMap += (("verticesRemoved", workerStats.verticesRemoved))

    statsMap += (("numberOfEdges", workerStats.outgoingEdgesAdded - workerStats.outgoingEdgesRemoved))
    statsMap += (("edgesAdded", workerStats.outgoingEdgesAdded))
    statsMap += (("edgesRemoved", workerStats.outgoingEdgesRemoved))

    statsMap += (("graphLoadingWaitInMilliseconds", (graphLoadingWait / 1000000.0).toLong))
    statsMap += (("jvmCpuTimeInMilliseconds", (totalJvmCpuTime / 1000000.0).toLong))
    statsMap += (("computationTimeInMilliseconds", (totalTime / 1000000.0).toLong))

    val computationStatistics = new DefaultComputationStatistics(statsMap)
    computationStatistics
  }

  protected def performComputation

  def getJVMCpuTime = {
    val bean = ManagementFactory.getOperatingSystemMXBean
    if (!bean.isInstanceOf[OperatingSystemMXBean]) {
      0
    } else {
      (bean.asInstanceOf[OperatingSystemMXBean]).getProcessCpuTime
    }
  }
}