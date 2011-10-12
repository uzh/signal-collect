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
 *
 *  @param neutralElement The neutral element of the aggregation operation: `operation(x, neutralElement) == x`
 *
 *  @param operation The aggregation operation that is executed on the values that have been extracted from vertices by the `extractor` function.
 *
 *  @param extractor The function that extracts values of type `ValueType` from vertices.
 *
 *  @note There is no guarantee about the order in which the aggregation operations get executed on the vertices.
 *
 *  @note This is the most powerful and low-level aggregation operation. All other aggregation operations are specializations of this trait. 
 *
 *  @author Philip Stutz
 *  @version 1.0
 *  @since 1.0
 */
trait AggregationOperation[ValueType] {
  val neutralElement: ValueType
  def aggregate(a: ValueType, b: ValueType): ValueType
  def extract(v: Vertex): ValueType
}