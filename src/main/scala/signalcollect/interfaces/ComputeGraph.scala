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

object ComputeGraph {
  lazy val defaultNumberOfThreadsUsed: Int = Runtime.getRuntime.availableProcessors
  lazy val defaultSignalThreshold: Double = 0.01
  lazy val defaultCollectThreshold: Double = 0
}

trait ComputeGraph extends GraphApi {
  def execute: ComputationStatistics
  def recalculateScores
  def recalculateScoresForVertexId(vertexId: Any)
  def shutdown

  def forVertexWithId(vertexId: Any, f: (Vertex[_, _]) => Unit)
  def foreachVertex(f: (Vertex[_, _]) => Unit)

  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long

  def sum[N](implicit numeric: Numeric[N]): N
  def product[N](implicit numeric: Numeric[N]): N
  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType]
  def aggregateStates[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType): ValueType
  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType

  def setSignalThreshold(t: Double)
  def setCollectThreshold(t: Double)
  def setStepsLimit(l: Int)
  def setUndeliverableSignalHandler(h: (Signal[_,_,_], GraphApi) => Unit)
}


    





