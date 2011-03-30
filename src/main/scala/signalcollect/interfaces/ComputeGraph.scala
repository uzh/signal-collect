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

package signalcollect.interfaces

import signalcollect.api.AsynchronousComputeGraph

object ComputeGraph {
  lazy val defaultNumberOfThreads = Runtime.getRuntime.availableProcessors
}

trait ComputeGraph extends GraphApi {
  def execute: ComputationStatistics
  def shutDown

  def foreach(f: (Vertex[_, _]) => Unit)
  def foreach(f: PartialFunction[Vertex[_, _], Unit])
  
  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long
//  def countEdges: Long
  
  def sum[N](implicit numeric: Numeric[N]): N
  def product[N](implicit numeric: Numeric[N]): N
  def aggregate[ValueType](neutralElement: ValueType, aggregator: (ValueType, ValueType) => ValueType): ValueType
  def customAggregate[ValueType](neutralElement: ValueType, aggregator: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType

  def setSignalThreshold(t: Double)
  def setCollectThreshold(t: Double)
  def setStepsLimit(l: Int) 
}