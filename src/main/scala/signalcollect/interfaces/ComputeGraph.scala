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


  /*
   * Returns the number of vertices that are instances of @VertexType.
   */
  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long = {
    val matchingTypeCounter: (Vertex[_, _]) => Long = { v: Vertex[_, _] =>
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

  /*
   * Returns the sum of all numeric states.
   */
  def sum[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.zero, { (x: N, y: N) => numeric.plus(x, y) })
  }

  /*
   * Returns the product of all numeric states.
   */
  def product[N](implicit numeric: Numeric[N]): N = {
    aggregateStates(numeric.one, { (x: N, y: N) => numeric.times(x, y) })
  }

  /*
   * Returns some aggregate of all states of type @ValueType calculated by the
   * associative function @operation. Returns none if there is no state of that
   * type. 
   */
  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType] = {
    val stateExtractor: (Vertex[_, _]) => Option[ValueType] = { v: Vertex[_, _] =>
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

  /*
   * Returns the aggregate of all states calculated by the associative @operation function.
   * States that are not instances of @ValueType get mapped to of @neutralElement.
   */
  def aggregateStates[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType): ValueType = {
    val stateExtractor: (Vertex[_, _]) => ValueType = { v: Vertex[_, _] =>
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
  
  /*
   * Returns the aggregate of all states calculated by applying the associative @operation function to
   * the values that have been extracted by the @extractor function from vertex states. The function needs to have a
   * neutral element @neutralElement.
   */
  def customAggregate[ValueType](neutralElement: ValueType, operation: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType): ValueType

  def setSignalThreshold(t: Double)
  def setCollectThreshold(t: Double)
  def setStepsLimit(l: Int)
  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit)
}


    





