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

import scala.collection.mutable.PriorityQueue
import scala.reflect.ClassTag
import scala.util.Sorting

import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.ModularAggregationOperation

/**
 *  Builds a map with the vertex ids as keys and the vertex states as values.
 *
 *  Only works on graphs where this information fits into memory.
 */
case class IdStateMapAggregator[IdType, StateType]() extends ModularAggregationOperation[Map[IdType, StateType]] {
  val neutralElement = Map[IdType, StateType]()
  def extract(v: Vertex[_, _]): Map[IdType, StateType] = {
    try {
      Map[IdType, StateType]((v.id.asInstanceOf[IdType], v.state.asInstanceOf[StateType]))
    }
  }
  def aggregate(a: Map[IdType, StateType], b: Map[IdType, StateType]): Map[IdType, StateType] = a ++ b
}

/**
 *  Aggregation operation that sums up all the vertex states that have numeric type `N`.
 */
case class SumOfStates[N: Numeric: Manifest]() extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   *  Sums up the values
   */
  def operation(a: N, b: N): N = numeric.plus(a, b)

}

/**
 *  Aggregation operation that multiplies all the vertex states that have numeric type `N`.
 */
case class ProductOfStates[N: Numeric: Manifest]() extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   *  Multiplies the values
   */
  def operation(a: N, b: N): N = numeric.times(a, b)

}

/**
 *  Aggregation operation that returns a sample of all vertex ids.
 */
case class SampleVertexIds(sampleSize: Int) extends ModularAggregationOperation[List[Any]] {

  val neutralElement = List[Any]()

  def aggregate(a: List[Any], b: List[Any]): List[Any] = {
    val combinedList = a ++ b
    combinedList.slice(0, math.min(sampleSize, combinedList.size))
  }

  def extract(v: Vertex[_, _]): List[Any] = {
    List(v.id)
  }
}

/**
 *  Aggregation operation that counts the number of vertices in this graph that have type `VertexType`.
 *
 *  @example `val numberOfPageRankVertices = graph.aggregate(new CountVertices[PageRankVertex])`
 */
case class CountVertices[VertexType <: Vertex[_, _]: ClassTag]() extends ModularAggregationOperation[Long] {

  val neutralElement: Long = 0l

  /**
   *  Sums up the number of vertices of type `VertexType` that were found.
   */
  def aggregate(a: Long, b: Long): Long = a + b

  /**
   *  Returns 1 for vertices that match `VertexType`, 0 for other types
   */
  def extract(v: Vertex[_, _]): Long = {
    v match {
      case v: VertexType => 1l
      case other => 0l
    }
  }

}

/**
 *  Extracts states from vertices
 */
abstract class StateExtractor[StateType: Manifest] extends ModularAggregationOperation[Option[StateType]] {

  /**
   * Extracts the state from v if it matches `StateType`
   */
  def extract(v: Vertex[_, _]): Option[StateType] = {
    try {
      Some(v.state.asInstanceOf[StateType]) // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
    } catch {
      case _: Throwable => None
    }
  }

}

/**
 *  Calculates the aggregate of all vertex states of type `ValueType` using the associative function `operation`.
 */
abstract class ReduceStatesOperation[ValueType: Manifest] extends StateExtractor[ValueType] {

  def operation(a: ValueType, b: ValueType): ValueType

  val neutralElement = None

  def aggregate(a: Option[ValueType], b: Option[ValueType]): Option[ValueType] = {
    (a, b) match {
      case (Some(x), Some(y)) => Some(operation(x, y))
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (None, None) => None
    }
  }

}

/**
 *  Implements an extractor for aggregation operations that will operate on states of type `StateType`.
 */
abstract class StateAggregator[StateType] extends ModularAggregationOperation[StateType] {

  /**
   *  Extracts the state from v if it matches `StateType`
   */
  def extract(v: Vertex[_, _]): StateType = {
    try {
      v.state.asInstanceOf[StateType] // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
    } catch {
      case _: Throwable => neutralElement
    }
  }

}

/**
 * Finds the ids and states of the K vertices with the largest states.
 */
case class TopKFinder[State](k: Int)(implicit ord: Ordering[State])
  extends ComplexAggregation[Iterable[(_, State)], Iterable[(_, State)]] {

  implicit val ordering = Ordering.by((value: (_, State)) => value._2)

  def aggregationOnWorker(vertices: Stream[Vertex[_, _]]): Iterable[(_, State)] = {
    def extract(v: Vertex[_, _]): (_, State) = {
      v match {
        case vertex: Vertex[_, State] => (vertex.id, vertex.state)
      }
    }
    val threadName = Thread.currentThread.getName
    val values = (vertices map (extract(_)))
    selectTopK(k, values)
  }

  protected def selectTopK[G: ClassTag](k: Int, items: Stream[G])(implicit ord: Ordering[G]): Iterable[G] = {
    val startTime = System.currentTimeMillis
    var counter = 0
    val topK = new PriorityQueue[G]()(ord.reverse)
    for (item <- items) {
      counter += 1
      if (topK.size < k) {
        topK += item
      } else {
        if (ord.compare(topK.head, item) < 0) {
          topK.dequeue
          topK += item
        }
      }
    }
    topK.toArray[G]
  }

  def aggregationOnCoordinator(workerResults: Iterable[Iterable[(_, State)]]): Iterable[(_, State)] = {
    val startTime = System.currentTimeMillis
    def timePassedInSeconds = System.currentTimeMillis - System.currentTimeMillis
    val values = workerResults.toStream.flatMap(identity)
    val topK = (selectTopK(k, values)).toArray
    Sorting.quickSort[(_, State)](topK)(ordering.reverse)
    topK
  }

}
