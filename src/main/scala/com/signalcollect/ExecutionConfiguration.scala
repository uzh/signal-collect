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

import scala.language.existentials

import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.interfaces.ComplexAggregation

/**
 *  An execution configuration specifies execution parameters for a computation. This object
 *  represents an ExecutionConfiguration that is initialized with the default parameters.
 */
object ExecutionConfiguration extends ExecutionConfiguration(ExecutionMode.PureAsynchronous, 0.01, 0.0, None, None, None)

/**
 *  This configuration specifies execution parameters for a computation.
 *
 * 	@param executionMode Determines the way signal/collect operations are scheduled.
 * 	@param signalThreshold	A signal operation only gets executed if the signalScore of
 * 						a vertex is above this threshold.
 * 	@param collectThreshold A collect operation only gets executed if the collectScore of
 * 						a vertex is above this threshold.
 * 	@param timeLimit The computation duration is bounded by this value.
 * 	@param stepsLimit The maximum number of computation steps is bounded by this value.
 *
 *  @author Philip Stutz
 */
case class ExecutionConfiguration(
  executionMode: ExecutionMode.Value = ExecutionMode.PureAsynchronous,
  signalThreshold: Double = 0.01,
  collectThreshold: Double = 0.0,
  timeLimit: Option[Long] = None,
  stepsLimit: Option[Long] = None,
  globalTerminationCondition: Option[GlobalTerminationCondition[_]] = None) {

  /**
   *  Configures the execution mode used in a computation.
   *
   *  @param executionMode The execution mode used in a computation.
   */
  def withExecutionMode(executionMode: ExecutionMode.Value) = newExecutionConfiguration(executionMode = executionMode)

  /**
   *  Configures the signal threshold used in a computation.
   *
   *  @note If the signal score of a vertex is above the signal threshold, then the vertex will execute the `signal` operation in its edges.
   *
   *  @param signalThreshold The signal threshold used in a computation.
   */
  def withSignalThreshold(signalThreshold: Double) = newExecutionConfiguration(signalThreshold = signalThreshold)

  /**
   *  Configures the collect threshold used in a computation.
   *
   *  @note If the collect score of a vertex is above the collect threshold, then its `collect` operation will get executed.
   *
   *  @param collectThreshold The collect threshold used in a computation.
   */
  def withCollectThreshold(collectThreshold: Double) = newExecutionConfiguration(collectThreshold = collectThreshold)

  /**
   *  Configures the time limit in milliseconds used in a computation.
   *
   *  @param timeLimit The time limit in milliseconds used for a computation.
   */
  def withTimeLimit(timeLimit: Long) = {
    newExecutionConfiguration(timeLimit = Some(timeLimit))
  }

  /**
   *  Configures the maximum number of computation steps executed in a computation.
   *
   *  @note Only relevant for synchronous computations.
   *
   *  @param stepsLimit The maximum number of computation steps executed in a computation.
   */
  def withStepsLimit(stepsLimit: Long) = newExecutionConfiguration(stepsLimit = Some(stepsLimit))

  /**
   *  Configures the maximum number of computation steps executed in a computation.
   *
   *  @note Only relevant for synchronous computations.
   *
   *  @param stepsLimit The maximum number of computation steps executed in a computation.
   */
  def withGlobalTerminationCondition(globalTerminationCondition: GlobalTerminationCondition[_]) = newExecutionConfiguration(globalTerminationCondition = Some(globalTerminationCondition))

  /**
   *  Internal function to create a new configuration instance that defaults
   *  to parameters that are the same as the ones in this instance, unless explicitly set differently.
   */
  protected def newExecutionConfiguration(
    executionMode: ExecutionMode.Value = executionMode,
    signalThreshold: Double = signalThreshold,
    collectThreshold: Double = collectThreshold,
    timeLimit: Option[Long] = timeLimit,
    stepsLimit: Option[Long] = stepsLimit,
    globalTerminationCondition: Option[GlobalTerminationCondition[_]] = globalTerminationCondition): ExecutionConfiguration = {
    ExecutionConfiguration(
      executionMode = executionMode,
      signalThreshold = signalThreshold,
      collectThreshold = collectThreshold,
      timeLimit = timeLimit,
      stepsLimit = stepsLimit,
      globalTerminationCondition = globalTerminationCondition)
  }

  override def toString: String = {
    "Execution mode \t\t" + executionMode + "\n" +
      "Signal threshold \t" + signalThreshold + "\n" +
      "Collect threshold \t" + collectThreshold + "\n" +
      "Time limit \t\t" + timeLimit + "\n" +
      "Steps limit \t\t" + stepsLimit
  }
}

/**
 *  GlobalTerminationCondition defines a termination condition that depends on the global state of the graph.
 *  This class is abstract because the should terminate predicate on the aggregated value is not implemented.
 *
 *  @param aggregationOperation The aggregation operation used to compute the globally aggregated value
 *  @param aggregationInterval In a synchronous computation: aggregation interval in computation steps.
 *  						   In an asynchronous computation: aggregation interval in milliseconds
 *  @param shouldTerminate Function that takes a global aggregate and returns true iff the computation should
 *                         be terminated.
 */
case class GlobalTerminationCondition[ResultType](
  aggregationOperation: ComplexAggregation[_, ResultType],
  aggregationInterval: Long = 1000,
  shouldTerminate: ResultType => Boolean)
