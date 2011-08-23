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

/**
 *  Graph represents the entire Signal/Collect graph with its vertices and edges.
 *  It offers functions to execute computations and aggregation operations on this graph.
 *  Additionally it extends GraphEditor, which means that it offers functions to manipulate the graph.
 *
 *  @note This class is usually instantiated using the `GraphBuilder`
 *
 *  @see GraphBuilder, DefaultGraph
 *
 *  @example `val computeGraph = ComputeGraphBuilder.build`
 *
 *  @author Philip Stutz
 *  @version 1.0
 *  @since 1.0
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
   *  @usecase def forVertexWithId(vertexId: Any, f: Vertex => String): Option[String]
   */
  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType]

  /**
   *  Executes the function `f` on all vertices.
   *
   *  @note The function `f` may be executed in multiple other threads, beware of race conditions.
   *
   *  @note This function may be executed on other machines and references
   *  		to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def foreachVertex(f: Vertex => Unit)

  /**
   *  Counts the number of vertices in this compute graph that have type `VertexType`.
   *
   *  @return The number of vertices in this compute graph that have type `VertexType`.
   *
   *  @example `val numberOfPageVertices = computeGraph.countVertices[Page]`
   *
   *  @usecase def countVertices[Page]: Long
   */
  def countVertices[VertexType <: Vertex](implicit m: Manifest[VertexType]): Long = {
    val matchingTypeCounter: Vertex => Long = { v: Vertex =>
      {
        if (m.erasure.isInstance(v)) {
          1l
        } else {
          0l
        }
      }
    }
    customAggregate(0l, { (aggrValue: Long, vertexIncrement: Long) => aggrValue + vertexIncrement }, matchingTypeCounter)
  }

  /**
   *  Returns the sum of all vertex states that have type `N`.
   *
   *  @return The sum of all vertex states that have type `N`.
   *
   *  @note An instance of type class `Numeric[N]` needs to be implicitly available.
   *
   *  @example `val sumOfVertexStatesThatAreOfTypeDouble = computeGraph.sum[Double]`
   *
   *  @usecase def sum[Double]
   */
  def sum[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.zero, { (x: N, y: N) => numeric.plus(x, y) })
  }

  /**
   *  Returns the product of all vertex states that have type `N`.
   *
   *  @return The product of all vertex states that have type `N`.
   *
   *  @note An instance of type class `Numeric[N]` needs to be implicitly available.
   *
   *  @example `val productOfVertexStatesThatAreOfTypeDouble = computeGraph.product[Double]`
   *
   *  @usecase def product[Double]
   */
  def product[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.one, { (x: N, y: N) => numeric.times(x, y) })
  }

  /**
   *  Calculates the aggregate of all vertex states of type `ValueType` using the associative function `operation`.
   *
   *  @param operation An associative function that aggregates two instances of type `ValueType`
   *
   *  @return `Some` aggregate of all vertex states of type `ValueType` if at least one such instance can be found, `None` otherwise.
   *
   *  @example `val productOfVertexStatesThatAreOfTypeInt = computeGraph.reduce[Int](operation = (_ * _))`
   *
   *  @usecase def reduce[Int](operation: (Int, Int) => Int)
   */
  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType] = {
    val stateExtractor: Vertex => Option[ValueType] = { v: Vertex =>
      {
        try {
          Some(v.state.asInstanceOf[ValueType]) // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
        } catch {
          case _ => None
        }
      }
    }
    val optionOperation: (Option[ValueType], Option[ValueType]) => Option[ValueType] = { (a, b) =>
      (a, b) match {
        case (Some(x), Some(y)) => Some(operation(x, y))
        case (Some(x), None) => Some(x)
        case (None, Some(y)) => Some(y)
        case (None, None) => None
      }
    }
    customAggregate[Option[ValueType]](None, optionOperation, stateExtractor)
  }

  /**
   *  Calculates an aggregate of all vertex states of type `ValueType` and neutral elements that take the place of vertex states with incompatible types.
   *
   *  @param neutralElement The neutral element of the aggregation operation: `operation(x, neutralElement) == x`
   *
   *  @param operation The aggregation operation that is executed on the vertex states.
   *
   *  @return The aggregate of all vertex states.
   *
   *  @note There is no guarantee about the order in which the aggregation operations get executed on the vertices.
   *
   *  @example `val sumOfVertexStatesThatAreOfTypeInt = computeGraph.aggregateStates[Int](0, (_ + _))
   *
   *  @usecase def aggregateStates(neutralElement: Int, operation: (Int, Int) => Int)
   */
  def aggregateStates[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType): ValueType = {
    val stateExtractor: Vertex => ValueType = { v: Vertex =>
      {
        try {
          v.state.asInstanceOf[ValueType] // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
        } catch {
          case _ => neutralElement
        }
      }
    }
    customAggregate(neutralElement, operation, stateExtractor)
  }

  /**
   *  Calculates the aggregate of values that are extracted from vertices.
   *
   *  @param neutralElement The neutral element of the aggregation operation: `operation(x, neutralElement) == x`
   *
   *  @param operation The aggregation operation that is executed on the values that have been extracted from vertices by the `extractor` function.
   *
   *  @param extractor The function that extracts values of type `ValueType` from vertices.
   *
   *  @return The aggregate of all values that have been extracted from vertices and potentially some neutral elements.
   *
   *  @note There is no guarantee about the order in which the aggregation operations get executed on the vertices.
   *
   *  @note This is the most powerful and low-level aggregation function. All other aggregation functions get rewritten
   *  		to more specific executions of this function.
   *
   *  @example See implementations of other aggregation functions.
   */
  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: Vertex => ValueType): ValueType

  /**
   *  Sets the function that handles signals that could not be delivered to a vertex.
   *
   *  @note By default nothing happens when a signal is not deliverable. The handler function
   *  		receives the signal and an instance of GraphApi as parameters in order to take some
   *  		action that handles this case.
   */
  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphEditor) => Unit)
}


    





