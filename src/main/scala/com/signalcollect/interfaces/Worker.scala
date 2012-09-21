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
import akka.actor.Actor
import akka.dispatch.Future

trait Worker extends Actor with MessageRecipientRegistry with Logging {

  override def toString = this.getClass.getSimpleName
  def workerId: Int
  
  def processSignal(signal: SignalMessage[_])

  def addVertex(serializedVertex: Array[Byte]) // object should be created in the heap of the thread which uses its
  def addEdge(sourceId: Any, serializedEdge: Array[Byte]) // object should be created in the heap of the thread which uses its
  def addVertex(vertex: Vertex[_, _])
  def addEdge(sourceId: Any, edge: Edge[_])
  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_])
  def removeVertex(vertexId: Any)
  def removeEdge(edgeId: EdgeId)
  def removeVertices(shouldRemove: Vertex[_, _] => Boolean)
  def loadGraph(graphLoader: GraphEditor => Unit)

  def setUndeliverableSignalHandler(h: (SignalMessage[_], GraphEditor) => Unit)

  def setSignalThreshold(signalThreshold: Double)
  def setCollectThreshold(collectThreshold: Double)

  def recalculateScores
  def recalculateScoresForVertexWithId(vertexId: Any)

  def forVertexWithId[VertexType <: Vertex[_, _], ResultType](vertexId: Any, f: VertexType => ResultType): ResultType
  def foreachVertex(f: Vertex[_, _] => Unit)

  def aggregate[ValueType](aggregationOperation: AggregationOperation[ValueType]): ValueType

  def pauseAsynchronousComputation
  def startAsynchronousComputation

  def signalStep
  def collectStep: Boolean

  def getWorkerStatistics: WorkerStatistics

  def shutdown

}
