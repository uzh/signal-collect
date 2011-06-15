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

  /* 
   * Starts the execution of the computation. The method blocks until the computation has ended.
   * 
   * There are three reasons why a computation may end:
   *  - All signal/collect scores are below the thresholds
   *  - Steps limit reached (only for synchronous compute graphs)
   *  - Time limit exceeded (not yet implemented)
   *  
   * It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.  
   */
  def execute: ComputationStatistics

  /* 
   * Recalculates the signal/collect scores of all vertices.
   * 
   * If the scores are above the respective thresholds, the signal/collect operations
   * will be executed when the computation is executed again.
   */
  def recalculateScores
  
  /* 
   * Recalculates the signal/collect scores of the vertex with the id @vertexId.
   * 
   * If the scores are above the respective thresholds, the signal/collect operations
   * will be executed when the computation is executed again.
   */
  def recalculateScoresForVertexId(vertexId: Any)
  
  /* 
   * Shuts down the compute graph and frees associated resources.
   * 
   * If other methods get called after shutdown, then the behavior is unpredictable.
   */
  def shutdown

  /* 
   * Executes the function @f on the vertex with id @vertexId.
   * 
   * The function @f may be executed in another thread, beware of race conditions.
   * In the future this function may also be executed on another machine and references
   * to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def forVertexWithId(vertexId: Any, f: (Vertex[_, _]) => Unit)
  
  /* 
   * Executes the function @f on all vertices.
   * 
   * The function @f may be executed in multiple other threads, beware of race conditions.
   * In the future this function may also be executed on other machines and references
   * to objects that are not reachable from the vertex-parameter may not be accessible.
   */
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
  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit)
}


    





