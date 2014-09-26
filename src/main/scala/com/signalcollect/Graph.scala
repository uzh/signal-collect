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

package com.signalcollect

import com.signalcollect.interfaces.ComplexAggregation
import com.signalcollect.interfaces.WorkerStatistics
import akka.actor.ActorSystem
import com.signalcollect.interfaces.AggregationOperation

/**
 *  Graph represents the entire Signal/Collect graph with its vertices and edges.
 *  It offers functions to execute computations and aggregation operations on this graph.
 *  Additionally it extends GraphEditor, which means that it offers functions to manipulate the graph.
 *
 *  @note This class is usually instantiated using the `GraphBuilder`
 *
 *  @see GraphBuilder, DefaultGraph
 *
 *  @example `val graph = GraphBuilder.build`
 *
 *  @author Philip Stutz
 */
trait Graph[Id, Signal] extends GraphEditor[Id, Signal] {

  def numberOfNodes: Int
  def numberOfWorkers: Int

  /**
   *  Starts the execution of the computation using the default execution parameters and
   *  returns information about the execution. The method blocks until the computation has ended.
   *
   *  @return Information about the configuration and statistics about this execution.
   *
   *  @note Blocks during execution.
   *
   *  @note Termination: There are three reasons why a computation may end:
   *  			- All signal/collect scores are below the thresholds
   *  			- Steps limit reached (for synchronous compute graphs)
   *  			- Time limit exceeded (not yet implemented)
   *
   *  @note It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute: ExecutionInformation[Id, Signal]

  /**
   *  Starts the execution of the computation using the default execution parameters and
   *  returns information about the execution. The method blocks until the computation has ended.
   *
   *  @param executionConfiguration Specifies configuration parameters that influence the execution.
   *
   *  @return Information about the configuration and statistics about this execution.
   *
   *  @note Blocks during execution.
   *
   *  @note Termination: There are three reasons why a computation may end:
   *  			- All signal/collect scores are below the thresholds
   *  			- Steps limit reached (for synchronous compute graphs)
   *  			- Time limit exceeded (not yet implemented)
   *
   *  @note It may make sense to call this method repeatedly, for example if a compute graph is modified after execution.
   */
  def execute(executionConfiguration: ExecutionConfiguration[Id, Signal]): ExecutionInformation[Id, Signal]

  /**
   *  Recalculates the signal/collect scores of all vertices.
   *
   *  @note If the scores are above the respective thresholds, the signal/collect operations
   * 		will be executed when the computation is executed again.
   *
   *  @note This operation is meant to be used after the foreachVertex operation in case
   * 		the vertex signal/collect scores have changed.
   *
   *  @see `foreachVertex`
   */
  def recalculateScores

  /**
   *  Waits until all processing has finished.
   *
   *  @note Only used with continuous asynchronous execution.
   */
  def awaitIdle

  /**
   *  Shuts down the compute graph and frees associated resources.
   *
   *  @note If methods on a ComputeGraph instance get called after having called `shutdown`, then the behavior is not specified.
   */
  def shutdown

  /**
   *  Executes the function `f` on the vertex with id `vertexId` and returns the result.
   *
   *  @return Returns the result of function `f`.
   *
   *  @note The function `f` may be executed in another thread or on another computer.
   *
   *  @note References to objects that are not reachable from the vertex passed to
   *  the function as a parameter may not be accessible or may be subject to race conditions.
   *
   *  @param f The function that gets executed on the vertex with id `vertexId`
   *
   *  @example `forVertexWithId(vertexId = 1, f = { v: Vertex[_, _, _, _] => v.state })`
   *
   *  @usecase def forVertexWithId(vertexId: Any, f: Vertex[_, _, _, _] => String): String
   */
  def forVertexWithId[VertexType <: Vertex[Id, _, Id, Signal], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType

  /**
   *  Executes the function `f` on all vertices.
   *
   *  @note The function `f` may be executed in multiple other threads, beware of race conditions.
   *
   *  @note This function may be executed on other machines and references
   *  		to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def foreachVertex(f: Vertex[Id, _, Id, Signal] => Unit)

  /**
   *  The worker passes a GraphEditor to function `f`, and then executes the resulting function on all vertices.
   *
   *  @note The resulting function may be executed in multiple other threads, beware of race conditions.
   *
   *  @note The resulting function may be executed on other machines and references
   *  		to objects that are not reachable from the vertex-parameter may not be accessible.
   */
  def foreachVertexWithGraphEditor(f: GraphEditor[Id, Signal] => Vertex[Id, _, Id, Signal] => Unit)

  /**
   *  Applies an aggregation operation to the graph and returns the result.
   *
   *  @param aggregationOperation The aggregation operation that will get executed on the graph
   *
   *  @return The result of the aggregation operation.
   *
   *  @note There is no guarantee about the order in which the aggregation operations get executed on the vertices.
   *
   *  @example See concrete implementations of other aggregation operations, i.e. `SumOfStates`.
   */
  def aggregate[ResultType](aggregationOperation: ComplexAggregation[_, ResultType]): ResultType

  /**
   * Simplified alternative API for aggregation operations.
   * 
   * @param map Function that extracts the relevant value from a vertex.
   * @param reduce Aggregation operation that is executed on the extracted values.
   * @param neutralElement Neutral element of the aggregation operation 'reduce'.
   *
   *  @example Computes sum of ranks: graph.mapReduce[PageRankVertex, Double](v => v.state, _ + _, 0)
   */
  def mapReduce[VertexType <: Vertex[_, _, _, _], ResultType](
    map: VertexType => ResultType,
    reduce: (ResultType, ResultType) => ResultType,
    neutralElement: ResultType): ResultType = {
    val r = reduce // Rename to avoid collision with the name of the inner function.
    val aggregation = new AggregationOperation[ResultType] {
      def extract(v: Vertex[_, _, _, _]): ResultType = {
        try {
          map(v.asInstanceOf[VertexType])
        } catch {
          case _: Throwable =>
            neutralElement
        }
      }
      def reduce(elements: Stream[ResultType]): ResultType = {
        elements.foldLeft(neutralElement)(r)
      }
    }
    val result = aggregate(aggregation)
    result
  }

  /**
   *  Resets operation statistics and removes all the vertices and edges in this graph.
   *  Leaves the message counters untouched.
   */
  def reset

  /**
   *  Returns the local internally used actor system of this this Signal/Collect graph.
   *
   *  @return the internal ActorSystem.
   */
  private[signalcollect] def system: ActorSystem

  /**
   *  Gathers worker statistics.
   *
   *  @return Various individual statistics from all workers.
   */
  private[signalcollect] def getWorkerStatistics: List[WorkerStatistics]

  /**
   * Creates a snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   * Overwrites any previous snapshot that might exist.
   */
  private[signalcollect] def snapshot

  /**
   * Restores the last snapshot of all the vertices in all workers.
   * Does not store the toSignal/toCollect collections or pending messages.
   * Should only be used when the workers are idle.
   */
  private[signalcollect] def restore

  /**
   * Deletes the worker snapshots if they exist.
   */
  private[signalcollect] def deleteSnapshot

}

/**
 * In order to unlock advanced methods on Graph, add this import to your program:
 * 		import com.signalcollect.ExtendedGraph._
 */
object ExtendedGraph {
  implicit class InternalGraph(g: Graph[_, _]) {
    /**
     *  Returns the local internally used actor system of this this Signal/Collect graph.
     *
     *  @return the internal ActorSystem.
     */
    def system: ActorSystem = g.system

    /**
     *  Gathers worker statistics.
     *
     *  @return Various individual statistics from all workers.
     */
    def getWorkerStatistics = g.getWorkerStatistics

    /**
     * Creates a snapshot of all the vertices in all workers.
     * Does not store the toSignal/toCollect collections or pending messages.
     * Should only be used when the workers are idle.
     * Overwrites any previous snapshot that might exist.
     */
    def snapshot = g.snapshot

    /**
     * Restores the last snapshot of all the vertices in all workers.
     * Does not store the toSignal/toCollect collections or pending messages.
     * Should only be used when the workers are idle.
     */
    def restore = g.restore

    /**
     * Deletes the worker snapshots if they exist.
     */
    def deleteSnapshot = g.deleteSnapshot
  }
}
