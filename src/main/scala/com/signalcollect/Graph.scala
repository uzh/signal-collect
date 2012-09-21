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

import com.signalcollect.interfaces.SignalMessage
import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.interfaces.AggregationOperation

/**
 *  Graph represents the entire Signal/Collect graph with its vertices and edges.
 *  It offers functions to execute computations and aggregation operations on this graph.
 *  Additionally it extends GraphEditor, which means that it offers functions to manipulate the graph.
 *
 *  @note This class is usually instantiated using the `GraphBuilder`
 *
 *  @see GraphBuilder, DefaultGraph
 *
 *  @example `val graph = GraphBuilder.build`
 *
 *  @author Philip Stutz
 */
trait Graph extends GraphEditor {

  /**
   *  Starts the execution of the computation using the default execution parameters and
   *  returns information about the execution. The method blocks until the computation has ended.
   *
   *  @return Information about the configuration and statistics about this execution.
   *
   *  @note Blocks during execution.
   *
   *  @note Termination: There are three reasons why a computation may end:
   *  			- All signal/collect scores are below the thresholds
   *  			- Steps limit reached (for synchronous compute graphs)
   *  			- Time limit exceeded (not yet implemented)
   *
   *  @note It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute: ExecutionInformation

  /**
   *  Starts the execution of the computation using the default execution parameters and
   *  returns information about the execution. The method blocks until the computation has ended.
   *
   *  @param executionConfiguration Specifies configuration parameters that influence the execution.
   *
   *  @return Information about the configuration and statistics about this execution.
   *
   *  @note Blocks during execution.
   *
   *  @note Termination: There are three reasons why a computation may end:
   *  			- All signal/collect scores are below the thresholds
   *  			- Steps limit reached (for synchronous compute graphs)
   *  			- Time limit exceeded (not yet implemented)
   *
   *  @note It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute(executionConfiguration: ExecutionConfiguration): ExecutionInformation

  /**
   *  Recalculates the signal/collect scores of all vertices.
   *
   *  @note If the scores are above the respective thresholds, the signal/collect operations
   * 		will be executed when the computation is executed again.
   *
   *  @note This operation is meant to be used after the foreachVertex operation in case
   * 		the vertex signal/collect scores have changed.
   *
   *  @see `foreachVertex`
   */
  def recalculateScores

  /**
   *  Recalculates the signal/collect scores of the vertex with the id @vertexId.
   *
   *  @param vertexId The vertex id of the vertex which should have its scores recalculated.
   *
   *  @note If the scores are above the respective thresholds, the signal/collect operations
   *  		will be executed when the computation is executed again.
   *
   *  @note This operation is meant to be used after the forVertexWithId operation in case
   * 		the vertex signal/collect scores have changed.
   *
   *  @see `forVertexWithId`
   */
  def recalculateScoresForVertexWithId(vertexId: Any)

  /**
   *  Waits until all processing has finished.
   *
   *  @note Only used with continuous asynchronous execution.
   */
  def awaitIdle
  
  /**
   *  Shuts down the compute graph and frees associated resources.
   *
   *  @note If methods on a ComputeGraph instance get called after having called `shutdown`, then the behavior is not specified.
   */
  def shutdown

  /**
   *  Executes the function `f` on the vertex with id `vertexId` and returns the result.
   *
   *  @return `Some` return value of the function `f` if the function was executed successfully, else `None`.
   *
   *  @note The function `f` may be executed in another thread or on another computer.
   *
   *  @note References to objects that are not reachable from the vertex passed to
   *  the function as a parameter may not be accessible or may be subject to race conditions.
   *
   *  @param f The function that gets executed on the vertex with id `vertexId`
   *
   *  @example `forVertexWithId(vertexId = 1, f = { v: Vertex => v.state })`
   *
   *  @usecase def forVertexWithId(vertexId: Any, f: Vertex => String): String
   */
  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): ResultType

  /**
   *  Executes the function `f` on all vertices.
   *
   *  @note The function `f` may be executed in multiple other threads, beware of race conditions.
   *
   *  @note This function may be executed on other machines and references
   *  		to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def foreachVertex(f: Vertex[_, _] => Unit)

  /**
   *  Applies an aggregation operation to the graph and returns the result.
   *
   *  @param aggregationOperation The aggregation operation that will get executed on the graph
   *
   *  @return The result of the aggregation operation.
   *
   *  @note There is no guarantee about the order in which the aggregation operations get executed on the vertices.
   *
   *  @example See concrete implementations of other aggregation operations, i.e. `SumOfStates`.
   */
  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]): ValueType

  /**
   *  Sets the function that handles signals that could not be delivered to a vertex.
   *
   *  @note By default nothing happens when a signal is not deliverable. The handler function
   *  		receives the signal and an instance of GraphEditor as parameters in order to take some
   *  		action that handles this case.
   */
  def setUndeliverableSignalHandler(h: (SignalMessage[_], GraphEditor) => Unit)
}


    





