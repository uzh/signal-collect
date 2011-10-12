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

import com.signalcollect.interfaces.AggregationOperation

class SumOfStates[N: Numeric: Manifest] extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   * Sums up the values
   */
  def operation(a: N, b: N): N = numeric.plus(a, b)

}

class ProductOfStates[N: Numeric: Manifest] extends ReduceStatesOperation[N] {

  val numeric = implicitly[Numeric[N]]

  /**
   * Multiplies the values
   */
  def operation(a: N, b: N): N = numeric.times(a, b)

}

/**
 *  Counts the number of vertices in this compute graph that have type `VertexType`.
 *
 *  @example `val numberOfPageRankVertices = graph.aggregate(new CountVertices[PageRankVertex])`
 *
 *  @usecase CountVertices[Vertex]
 */
class CountVertices[VertexType <: Vertex: Manifest] extends AggregationOperation[Long] {
  val m = manifest[VertexType]

  val neutralElement: Long = 0l

  /**
   * Multiplies the values
   */
  def aggregate(a: Long, b: Long): Long = a + b

  /**
   * Counts only vertices that match `VertexType`
   */
  def extract(v: Vertex): Long = {
    if (m.erasure.isInstance(v)) {
      1l
    } else {
      0l
    }
  }

}

/**
 *  Extracts states from vertices
 */
abstract class StateExtractor[StateType: Manifest] extends AggregationOperation[Option[StateType]] {

  /**
   * Extracts the state from v if it matches `StateType`
   */
  def extract(v: Vertex): Option[StateType] = {
    try {
      Some(v.state.asInstanceOf[StateType]) // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
    } catch {
      case _ => None
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

abstract class StateAggregator[StateType] extends AggregationOperation[StateType] {

  /**
   * Extracts the state from v if it matches `StateType`
   */
  def extract(v: Vertex): StateType = {
    try {
      v.state.asInstanceOf[StateType] // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
    } catch {
      case _ => neutralElement
    }
  }

}