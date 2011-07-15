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

package signalcollect.api

import signalcollect.interfaces._
import signalcollect.interfaces.MessageRecipient
import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.implementations.coordinator._
import signalcollect.configuration._

/**
 * Default [[signalcollect.interfaces.ComputeGraph]] implementation.
 */
class DefaultComputeGraph(val config: Configuration = DefaultLocalConfiguration(), workerApi: WorkerApi, coordinator: Coordinator) extends ComputeGraph with GraphApi {
  
  /** GraphApi */

  def execute: ExecutionInformation = execute(DefaultExecutionConfiguration)
  
  def execute(parameters: ExecutionConfiguration): ExecutionInformation = coordinator.execute(parameters)

  /** WorkerApi */

  def recalculateScores = workerApi.recalculateScores

  def recalculateScoresForVertexWithId(vertexId: Any) = workerApi.recalculateScoresForVertexWithId(vertexId)

  def shutdown = workerApi.shutdown

  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    workerApi.forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex[_, _]) => Unit) = workerApi.foreachVertex(f)

  def customAggregate[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType,
    extractor: (Vertex[_, _]) => ValueType): ValueType = {
    workerApi.customAggregate(neutralElement, operation, extractor)
  }

  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit) = workerApi.setUndeliverableSignalHandler(h)

  /** GraphApi */

  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) {
    workerApi.sendSignalToVertex(signal = signal, targetId = targetId, sourceId = sourceId)
  }

  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL) = workerApi.sendSignalToAllVertices(signal, sourceId)

  def addVertex(vertex: Vertex[_, _]) = workerApi.addVertex(vertex)

  def addEdge(edge: Edge[_, _]) = workerApi.addEdge(edge)

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_, _]) {
    workerApi.addPatternEdge(sourceVertexPredicate, edgeFactory)
  }

  def removeVertex(vertexId: Any) = workerApi.removeVertex(vertexId)

  def removeEdge(edgeId: (Any, Any, String)) = workerApi.removeEdge(edgeId)

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) = workerApi.removeVertices(shouldRemove)

}