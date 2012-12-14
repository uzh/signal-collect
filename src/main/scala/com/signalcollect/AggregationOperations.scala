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

import scala.Predef.Map.apply
import scala.Some.apply
import scala.collection.immutable.List.apply
import scala.reflect.ClassTag

import com.signalcollect.interfaces.AggregationOperation

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

/**
 *  Builds a map with the vertex ids as keys and the vertex states as values.
 *
 *  Only works on graphs where this information fits into memory.
 */
class IdStateMapAggregator[IdType, StateType] extends ModularAggregationOperation[Map[IdType, StateType]] {
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
class SumOfStates[N: Numeric: Manifest] extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   *  Sums up the values
   */
  def operation(a: N, b: N): N = numeric.plus(a, b)

}

/**
 *  Aggregation operation that multiplies all the vertex states that have numeric type `N`.
 */
class ProductOfStates[N: Numeric: Manifest] extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   *  Multiplies the values
   */
  def operation(a: N, b: N): N = numeric.times(a, b)

}

/**
 *  Aggregation operation that returns a sample of all vertex ids.
 */
class SampleVertexIds(sampleSize: Int) extends ModularAggregationOperation[List[Any]] {

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
class CountVertices[VertexType <: Vertex[_, _]: ClassTag] extends ModularAggregationOperation[Long] {

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
class TopKFinder[Id, State](k: Int, isFirstLargerThanSecond: (State, State) => Boolean) extends ModularAggregationOperation[List[(Id, State)]] {
  def extract(v: Vertex[_, _]): List[(Id, State)] = {
    v match {
      case vertex: Vertex[Id, State] => List((vertex.id, vertex.state))
    }
  }
  def aggregate(a: List[(Id, State)], b: List[(Id, State)]): List[(Id, State)] = {
    merge[(Id, State)](a, b, 0, k, { (first: (Id, State), second: (Id, State)) => isFirstLargerThanSecond(first._2, second._2) })
  }
  val neutralElement = List[(Id, State)]()

  final def merge[G](a: List[G], b: List[G], counter: Int, limit: Int, isFirstLargerThanSecond: (G, G) => Boolean): List[G] = {
    if (counter < limit) {
      b match {
        case Nil => a.take(limit - counter)
        case other => {
          a match {
            case Nil => b.take(limit - counter)
            case x :: xs => {
              if (isFirstLargerThanSecond(x, b.head)) {
                x :: merge(xs, b, counter + 1, limit, isFirstLargerThanSecond)
              } else {
                b.head :: merge(a, b.tail, counter + 1, limit, isFirstLargerThanSecond)
              }
            }
          }
        }
      }
    } else {
      Nil
    }
  }
}