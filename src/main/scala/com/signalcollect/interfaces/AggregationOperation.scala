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

package com.signalcollect.interfaces

import com.signalcollect.Vertex

/**
 *  An aggregation operation aggregates some value of type `ValueType` over all the vertices in a graph.
 */
trait AggregationOperation[ValueType] extends ComplexAggregation[ValueType, ValueType] {

  /**
   *  Extracts values of type `ValueType` from vertices.
   */
  def extract(v: Vertex[_, _]): ValueType

  /**
   * Reduces an arbitrary number of elements to one element.
   */
  def reduce(elements: Stream[ValueType]): ValueType

  def aggregationOnWorker(vertices: Stream[Vertex[_, _]]): ValueType = {
    reduce(vertices map extract)
  }

  def aggregationOnCoordinator(workerResults: Iterable[ValueType]): ValueType = {
    reduce(workerResults.toStream)
  }

}

/**
 * Implementation related interface.
 * Only use for more complex aggregations or when performance is important.
 */
trait ComplexAggregation[WorkerResult, EndResult] extends Serializable {

  def aggregationOnWorker(vertices: Stream[Vertex[_, _]]): WorkerResult

  def aggregationOnCoordinator(workerResults: Iterable[WorkerResult]): EndResult

}

/**
 *  More modular interface for aggregation operations.
 */
trait ModularAggregationOperation[ValueType] extends AggregationOperation[ValueType] {

  /**
   * Reduces an arbitrary number of elements to one element.
   */
  def reduce(elements: Stream[ValueType]): ValueType = {
    elements.foldLeft(neutralElement)(aggregate)
  }

  /**
   *  Aggregates all the values extracted by the `extract` function.
   *
   *  @note There is no guarantee about the order in which this function gets executed on the extracted values.
   */
  def aggregate(a: ValueType, b: ValueType): ValueType

  /**
   *  Neutral element of the `aggregate` function:
   *  `aggregate(x, neutralElement) == x`
   */
  def neutralElement: ValueType
}

