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

package com.signalcollect.configuration

import com.signalcollect.interfaces._

/**
 * This configuration specifies execution parameters for a computation. This object
 * represents an ExecutionConfiguration that is initialized with the default parameters.
 */
object DefaultExecutionConfiguration extends ExecutionConfiguration()

/**
 * This configuration specifies execution parameters for a computation:
 * 	executionMode:		Determines the way signal/collect operations are scheduled.
 * 	signalThreshold:	A signal operation only gets executed if the signalScore of
 * 						a vertex is above this threshold.
 * 	collectThreshold:	A collect operation only gets executed if the collectScore of
 * 						a vertex is above this threshold.
 * 	timeLimit:			The computation duration is bounded by this value.
 * 	stepsLimit:			The maximum number of computation steps is bounded by this value.
 */
case class ExecutionConfiguration(
  executionMode: ExecutionMode = OptimizedAsynchronousExecutionMode,
  signalThreshold: Double = 0.01,
  collectThreshold: Double = 0.0,
  timeLimit: Option[Long] = None,
  stepsLimit: Option[Long] = None) {

  def withExecutionMode(executionMode: ExecutionMode) = newExecutionConfiguration(executionMode = executionMode)
  def withSignalThreshold(signalThreshold: Double) = newExecutionConfiguration(signalThreshold = signalThreshold)
  def withCollectThreshold(collectThreshold: Double) = newExecutionConfiguration(collectThreshold = collectThreshold)
  def withTimeLimit(timeLimit: Option[Long]) = newExecutionConfiguration(timeLimit = timeLimit)
  def withStepsLimit(stepsLimit: Option[Long]) = newExecutionConfiguration(stepsLimit = stepsLimit)

  private def newExecutionConfiguration(
    executionMode: ExecutionMode = executionMode,
    signalThreshold: Double = signalThreshold,
    collectThreshold: Double = collectThreshold,
    timeLimit: Option[Long] = timeLimit,
    stepsLimit: Option[Long] = stepsLimit): ExecutionConfiguration = ExecutionConfiguration(
    executionMode = executionMode, signalThreshold = signalThreshold, collectThreshold = collectThreshold, timeLimit = timeLimit, stepsLimit = stepsLimit)

  override def toString: String = {
    "execution mode" + "\t" + "\t" + executionMode + "\n" +
      "signal threshold" + "\t" + signalThreshold + "\n" +
      "collect threshold" + "\t" + collectThreshold + "\n" +
      "time limit" + "\t" + "\t" + timeLimit + "\n" +
      "steps limit" + "\t" + "\t" + stepsLimit
  }
}

/**
 * An execution mode specifies the order in which signal/collect
 * operations get scheduled. There are currently three supported execution modes:
 * 	- SynchronousExecutionMode
 *  - PureAsynchronousExecutionMode
 *  - OptimizedAsynchronousExecutionMode
 */
sealed trait ExecutionMode extends Serializable

/**
 * In the synchronous execution mode there are computation steps.
 * Each computation step consists of a signal phase and a collect phase.
 * During the signal phase the signal function of all vertices that have signal scores
 * that are above the signal threshold get executed.
 * During the collect phase the collect function of all vertices that have collect scores
 * that are above the collect threshold get executed.
 *
 * In this execution mode there is a global synchronization between these phases and
 * between consecutive computation steps. This ensures that the signal phase and collect phase
 * of different vertices never overlap. This execution mode is related to the
 * Bulk Synchronous Parallel (BSP) paradigm and similar to Google Pregel.
 */
object SynchronousExecutionMode extends ExecutionMode {
  def self = this
  override def toString = "SynchronousExecutionMode"
}

/**
 * In the asynchronous execution mode there are no guarantees at all about the
 * order in which the signal/collect operations on vertices get executed. In practice
 * vertices will try to eagerly propagate information as quickly as possible.
 *
 * Depending on the algorithm, an asynchronous execution schedule may perform better,
 * because it has the potential to propagate information across the graph faster and
 * because it is less susceptible to oscillations.
 */
object PureAsynchronousExecutionMode extends ExecutionMode {
  override def toString = "PureAsynchronousExecutionMode"
}

/**
 * This is the default execution mode.
 *
 * In optimized asynchronous execution mode there is one synchronous signal operation
 * before switching to an asynchronous execution schedule.
 *
 * For some algorithms this enhances the performance of an asynchronous execution,
 * because during a purely asynchronous execution vertices collect before having received
 * the first signal from all their neighbors. In algorithms like PageRank this hurts
 * performance and it is avoided by this execution mode.
 */
object OptimizedAsynchronousExecutionMode extends ExecutionMode {
  override def toString = "OptimizedAsynchronousExecutionMode"
}