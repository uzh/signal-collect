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

import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.configuration.TerminationReason

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
  parameters: ExecutionConfiguration,
  executionStatistics: ExecutionStatistics,
  aggregatedWorkerStatistics: WorkerStatistics,
  individualWorkerStatistics: List[WorkerStatistics]) {

  override def toString: String = {
    "\n----------\n" +
      "- Worker -\n" +
      "----------" +
      "\n# workers \t" + config.numberOfWorkers + "\n" +
      config.workerConfiguration.toString + "\n" +
      "\n------------------------\n" +
      "- Execution Parameters -\n" +
      "------------------------\n" +
      parameters.toString + "\n" +
      "\n--------------\n" +
      "- Statistics -\n" +
      "--------------\n" +
      executionStatistics.toString + "\n" +
      aggregatedWorkerStatistics.toString + "\n"
  }
}

case class ExecutionStatistics(
  signalSteps: Long,
  collectSteps: Long,
  computationTimeInMilliseconds: Long,
  totalExecutionTimeInMilliseconds: Long, // should approximately equal computation time + idle waiting + garbage collection
  jvmCpuTimeInMilliseconds: Long,
  graphIdleWaitingTimeInMilliseconds: Long,
  preExecutionGcTimeInMilliseconds: Long,
  terminationReason: TerminationReason.Value) {

  override def toString: String = {
    "# signal steps" + "\t" + "\t" + signalSteps + "\n" +
      "# collect steps" + "\t" + "\t" + collectSteps + "\n" +
      "computation time (ms)" + "\t" + computationTimeInMilliseconds + "\n" +
      "JVM CPU time (ms)" + "\t" + jvmCpuTimeInMilliseconds + "\n" +
      "idle waiting (ms) (graph loading)" + "\t" + graphIdleWaitingTimeInMilliseconds + "\n" +
      "termination reason" + "\t" + terminationReason
  }

}

