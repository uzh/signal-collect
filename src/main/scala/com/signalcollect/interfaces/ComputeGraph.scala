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

package com.signalcollect.interfaces

import com.signalcollect.api._
import com.signalcollect.configuration._

/**
 * A ComputeGraph represents the entire Signal/Collect graph with its vertices and edges.
 *
 * It offers functions to execute computations and aggregation operations
 * on this graph. Additionally it extends GraphApi, which means that it
 * offers functions to manipulate the graph.
 */
trait ComputeGraph extends GraphApi {

  /**
   * Starts the execution of the computation using the default execution parameters,
   * returns information about the execution. The method blocks until the computation has ended.
   *
   * There are three reasons why a computation may end:
   *  - All signal/collect scores are below the thresholds
   *  - Steps limit reached (for synchronous compute graphs)
   *  - Time limit exceeded (not yet implemented)
   *
   * It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute: ExecutionInformation

  /**
   * Starts the execution of the computation using the default execution parameters,
   * returns information about the execution. The method blocks until the computation has ended.
   *
   * There are three reasons why a computation may end:
   *  - All signal/collect scores are below the thresholds
   *  - Steps limit reached (for synchronous compute graphs)
   *  - Time limit exceeded (not yet implemented)
   *
   * @executionConfiguration: The execution configuration determines the way a computation is executed.
   * Please see {@link ExecutionConfiguration} for details.
   *
   * It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute(executionConfiguration: ExecutionConfiguration): ExecutionInformation

  /**
   * Recalculates the signal/collect scores of all vertices.
   *
   * If the scores are above the respective thresholds, the signal/collect operations
   * will be executed when the computation is executed again.
   *
   * This operation is meant to be used after the foreachVertex operation in case
   * the vertex signal/collect scores have changed.
   */
  def recalculateScores

  /**
   * Recalculates the signal/collect scores of the vertex with the id @vertexId.
   *
   * If the scores are above the respective thresholds, the signal/collect operations
   * will be executed when the computation is executed again.
   *
   * This operation is meant to be used after the forVertexWithId operation in case
   * the vertex signal/collect scores have changed.
   */
  def recalculateScoresForVertexWithId(vertexId: Any)

  /**
   * Shuts down the compute graph and frees associated resources.
   *
   * If other methods get called after shutdown, then the behavior is unpredictable.
   */
  def shutdown

  /**
   * Executes the function @f on the vertex with id @vertexId.
   *
   * The function @f may be executed in another thread or on another computer.
   * BEWARE: References to objects that are not reachable from the vertex passed to
   * the function as a parameter may not be accessible or may be subject to race conditions.
   *
   * If the vertex is found, Some(result) of the function is returned, else None is returned.
   */
  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType]

  /**
   * Executes the function @f on all vertices.
   *
   * The function @f may be executed in multiple other threads, beware of race conditions.
   * In the future this function may also be executed on other machines and references
   * to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def foreachVertex(f: (Vertex) => Unit)

  /**
   * Returns the number of vertices that are instances of @VertexType.
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
   * Returns the sum of all numeric states.
   */
  def sum[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.zero, { (x: N, y: N) => numeric.plus(x, y) })
  }

  /**
   * Returns the product of all numeric states.
   */
  def product[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.one, { (x: N, y: N) => numeric.times(x, y) })
  }

  /**
   * Returns some aggregate of all states of type @ValueType calculated by the
   * associative function @operation. Returns none if there is no state of that
   * type.
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
   * Returns the aggregate of all states calculated by the associative @operation function.
   * States that are not instances of @ValueType get mapped to of @neutralElement.
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
   * Returns the aggregate of all states calculated by applying the associative @operation function to
   * the values that have been extracted by the @extractor function from vertex states. The function needs to have a
   * neutral element @neutralElement.
   */
  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: Vertex => ValueType): ValueType

  /**
   * Sets the function that handles signals that could not be delivered to a vertex.
   *
   * By default nothing happens when a signal is not deliverable. The handler function
   * receives the signal and a graph api as parameters in order to take some
   * action that handles this case.
   */
  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphApi) => Unit)
}


    





