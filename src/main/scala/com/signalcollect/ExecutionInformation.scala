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

package com.signalcollect

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.configuration.TerminationReason
import com.signalcollect.interfaces.WorkerStatistics

/**
 *  An instance of ExecutionInformation reports information such as execution statistics
 *  and configuration parameters related to an execution of a computation on a graph.
 *
 *  @param config The graph configuration of the graph that executed a computation.
 *  @param parameters The execution configuration for this particular execution.
 *  @param executionStatistics Statistics about this execution, such as the computation time,
 *  @param aggregatedWorkerStatistics Aggregated statistics over all the workers.
 *  @param individualWorkerStatistics A list of statistics for all workers individually.
 *
 *  @author Philip Stutz
 */
case class ExecutionInformation(
  config: GraphConfiguration,
  numberOfWorkers: Int,
  parameters: ExecutionConfiguration,
  executionStatistics: ExecutionStatistics,
  aggregatedWorkerStatistics: WorkerStatistics,
  individualWorkerStatistics: List[WorkerStatistics]) {

  override def toString: String = {
    "------------------------\n" +
      "- Execution Parameters -\n" +
      "------------------------\n" +
      parameters.toString + "\n" +
      "\n--------------\n" +
      "- Execution Statistics -\n" +
      "--------------\n" +
      executionStatistics.toString + "\n" +
      aggregatedWorkerStatistics.toString + "\n"
  }
}

case class ExecutionStatistics(
  var signalSteps: Long = 0,
  var collectSteps: Long = 0,
  var computationTime: Duration = Duration.create(0, TimeUnit.MILLISECONDS),
  var totalExecutionTime: Duration = Duration.create(0, TimeUnit.MILLISECONDS), // should approximately equal computation time + idle waiting + garbage collection
  var jvmCpuTime: Duration = Duration.create(0, TimeUnit.MILLISECONDS),
  var terminationReason: TerminationReason.Value = TerminationReason.Converged) {

  override def toString: String = {
    "# signal steps \t\t" + signalSteps + "\n" +
      "# collect steps \t" + collectSteps + "\n" +
      "Computation time \t" + computationTime.toUnit(TimeUnit.MILLISECONDS).toInt + " milliseconds\n" +
      "JVM CPU time \t\t" + jvmCpuTime.toUnit(TimeUnit.MILLISECONDS).toInt + " milliseconds\n" +
      "Termination reason \t" + terminationReason
  }

}

