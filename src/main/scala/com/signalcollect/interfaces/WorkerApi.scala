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
import scala.concurrent.Future

trait WorkerApi[Id, Signal] {

  def addVertex(vertex: Vertex[Id, _])
  def addEdge(sourceId: Id, edge: Edge[Id])
  def removeVertex(vertexId: Id)
  def removeEdge(edgeId: EdgeId[Id])
  def processSignal(signal: Signal, targetId: Id, sourceId: Option[Id])
  def modifyGraph(graphModification: GraphEditor[Id, Signal] => Unit, vertexIdHint: Option[Id] = None)

  def setUndeliverableSignalHandler(h: (Signal, Id, Option[Id], GraphEditor[Id, Signal]) => Unit)

  def setSignalThreshold(signalThreshold: Double)
  def setCollectThreshold(collectThreshold: Double)

  def recalculateScores
  def recalculateScoresForVertexWithId(vertexId: Id)

  def forVertexWithId[VertexType <: Vertex[Id, _], ResultType](vertexId: Id, f: VertexType => ResultType): ResultType
  def foreachVertex(f: Vertex[Id, _] => Unit)

  def aggregateOnWorker[WorkerResult](aggregationOperation: ComplexAggregation[WorkerResult, _]): WorkerResult
  def aggregateAll[WorkerResult, EndResult](aggregationOperation: ComplexAggregation[WorkerResult, EndResult]): EndResult

  def pauseComputation
  def startComputation

  def signalStep: Boolean
  def collectStep: Boolean

  def getWorkerStatistics: WorkerStatistics
  def getIndividualWorkerStatistics: List[WorkerStatistics]

  def shutdown
  def reset

}
