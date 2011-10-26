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

package com.signalcollect.interfaces

import com.signalcollect._

trait Worker extends MessageRecipient[Any] with MessageRecipientRegistry with Logging {
  def workerId: Int
  def messageBus: MessageBus[Any]
  
  override def toString = this.getClass.getSimpleName

  /**
   * initialization method for the worker (to start thread/actor)
   */
  def initialize
  
  def addVertex(vertex: Vertex)
  def addOutgoingEdge(edge: Edge)
  def addIncomingEdge(edge: Edge)
  def addPatternEdge(sourceVertexPredicate: Vertex => Boolean, edgeFactory: Vertex => Edge)
  def removeVertex(vertexId: Any)
  def removeOutgoingEdge(edgeId: EdgeId[Any, Any])
  def removeIncomingEdge(edgeId: EdgeId[Any, Any])
  def removeVertices(shouldRemove: Vertex => Boolean)

  def setUndeliverableSignalHandler(h: (SignalMessage[_, _, _], GraphEditor) => Unit)

  def setSignalThreshold(signalThreshold: Double)
  def setCollectThreshold(collectThreshold: Double)

  def recalculateScores
  def recalculateScoresForVertexWithId(vertexId: Any)

  def forVertexWithId[VertexType <: Vertex, ResultType](vertexId: Any, f: VertexType => ResultType): Option[ResultType]
  def foreachVertex(f: Vertex => Unit)

  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]): ValueType

  def pauseComputation
  def startComputation

  def signalStep
  def collectStep: Boolean

  def getWorkerStatistics: WorkerStatistics
  
  def shutdown

}