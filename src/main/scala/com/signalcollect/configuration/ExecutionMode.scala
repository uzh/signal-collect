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

/**
 *  An execution mode specifies the order in which signal/collect
 *  operations get scheduled. There are currently four supported execution modes:
 * 		- SynchronousExecutionMode
 *  	- PureAsynchronousExecutionMode
 *  	- OptimizedAsynchronousExecutionMode
 *  	- ContinuousAsynchronousExecutionMode
 *
 *  This class should really be a sealed trait with the objects defined in the same file.
 *  Unfortunately this would be ugly when used from Java, see
 *  http://stackoverflow.com/questions/2561415/how-do-i-get-a-scala-case-object-from-java
 */
object ExecutionMode extends Enumeration with Serializable {

  /**
   *  In the synchronous execution mode there are computation steps.
   *  Each computation step consists of a signal phase and a collect phase.
   *  During the signal phase the signal function of all vertices that have signal scores
   *  that are above the signal threshold get executed.
   *  During the collect phase the collect function of all vertices that have collect scores
   *  that are above the collect threshold get executed.
   *
   *  In this execution mode there is a global synchronization between these phases and
   *  between consecutive computation steps. This ensures that the signal phase and collect phase
   *  of different vertices never overlap. This execution mode is related to the
   *  Bulk Synchronous Parallel (BSP) paradigm and similar to Google Pregel.
   */
  val Synchronous = Value

  /**
   *  In the asynchronous execution mode there are no guarantees at all about the
   *  order in which the signal/collect operations on vertices get executed. In practice
   *  vertices will try to eagerly propagate information as quickly as possible.
   *
   *  Depending on the algorithm, an asynchronous execution schedule may perform better,
   *  because it has the potential to propagate information across the graph faster and
   *  because it is less susceptible to oscillations.
   */
  val PureAsynchronous = Value

  /**
   *  This is the default execution mode.
   *
   *  In optimized asynchronous execution mode there is one synchronous signal operation
   *  before switching to an asynchronous execution schedule.
   *
   *  For some algorithms this enhances the performance of an asynchronous execution,
   *  because during a purely asynchronous execution vertices collect before having received
   *  the first signal from all their neighbors. In algorithms like PageRank this hurts
   *  performance and it is avoided by this execution mode.
   */
  val OptimizedAsynchronous = Value

  /**
   *  Same as asynchronous but keeps on running even when the computation has stalled.
   *  Can be used for use cases such as continuous querying.
   */
  val ContinuousAsynchronous = Value

  /**
   *  Execution mode that allows to control execution using the computation manager.
   *  The computation manager is enabled by initializing the graph with:
   *  @example `val graph = GraphBuilder.withConsole(true)`
   *  and it is by default accessible @ http://localhost:8080
   */
  val Interactive = Value

}