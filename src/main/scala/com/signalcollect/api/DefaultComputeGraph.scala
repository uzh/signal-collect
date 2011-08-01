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

package com.signalcollect.api

import com.signalcollect.interfaces._
import com.signalcollect.interfaces.MessageRecipient
import com.signalcollect.implementations.coordinator._
import com.signalcollect.configuration._

/**
 * Default [[com.signalcollect.interfaces.ComputeGraph]] implementation.
 */
class DefaultComputeGraph(val config: Configuration = DefaultLocalConfiguration(), workerApi: WorkerApi, coordinator: Coordinator) extends ComputeGraph with GraphApi {
  
  /** GraphApi */

  def execute: ExecutionInformation = execute(DefaultExecutionConfiguration)
  
  def execute(parameters: ExecutionConfiguration): ExecutionInformation = coordinator.execute(parameters)

  /** WorkerApi */

  def recalculateScores = workerApi.recalculateScores

  def recalculateScoresForVertexWithId(vertexId: Any) = workerApi.recalculateScoresForVertexWithId(vertexId)

  def shutdown = workerApi.shutdown

  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType] = {
    workerApi.forVertexWithId(vertexId, f)
  }

  def foreachVertex(f: (Vertex) => Unit) = workerApi.foreachVertex(f)

  def customAggregate[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType,
    extractor: (Vertex) => ValueType): ValueType = {
    workerApi.customAggregate(neutralElement, operation, extractor)
  }

  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphApi) => Unit) = workerApi.setUndeliverableSignalHandler(h)

  /** GraphApi */

  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) {
    workerApi.sendSignalToVertex(signal = signal, targetId = targetId, sourceId = sourceId)
  }

  def addVertex(vertex: Vertex) = workerApi.addVertex(vertex)

  def addEdge(edge: Edge) = workerApi.addEdge(edge)

  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge) {
    workerApi.addPatternEdge(sourceVertexPredicate, edgeFactory)
  }

  def removeVertex(vertexId: Any) = workerApi.removeVertex(vertexId)

  def removeEdge(edgeId: (Any, Any, String)) = workerApi.removeEdge(edgeId)

  def removeVertices(shouldRemove: Vertex => Boolean) = workerApi.removeVertices(shouldRemove)

}