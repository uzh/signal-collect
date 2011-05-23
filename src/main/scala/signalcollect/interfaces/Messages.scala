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

// sender id used to indicate that a signal was sent from an external source
object EXTERNAL { override val toString = "EXTERNAL" }

// target id used to indicate that a signal should be delivered to all vertices
object ALL { override val toString = "ALL" }

// algorithm-specific message
case class Signal[+SourceIdType, +TargetIdType, +SignalType](sourceId: SourceIdType, targetId: TargetIdType, signal: SignalType)

case class CommandAddVertexFromFactory(vertexClass: Class[_ <: Vertex[_, _]], parameters: Seq[AnyRef]) {
  override def toString = {
    "CommandAddVertexFromFactory(" + vertexClass.getSimpleName + ", " + parameters.toString + ")"
  }
}
case class CommandAddEdgeFromFactory(edgeClass: Class[_ <: Edge[_, _]], parameters: Seq[AnyRef]) {
  override def toString = {
    "CommandAddEdgeFromFactory(" + edgeClass.getSimpleName + ", " + parameters.toString + ")"
  }
}

case class CommandAddVertex(vertex: Vertex[_, _])
case class CommandAddEdge(edge: Edge[_, _])

case class CommandAddIncomingEdge(edgeId: (Any, Any, String))

case class CommandAddPatternEdge[IdType](sourceVertexPredicate: Vertex[IdType, _] => Boolean, edgeFactory: IdType => Edge[IdType, _])

case class CommandRemoveVertex(vertexId: Any)
case class CommandRemoveOutgoingEdge(edgeId: (Any, Any, String))
case class CommandRemoveIncomingEdge(edgeId: (Any, Any, String))

case class CommandRemoveVertices(shouldRemove: Vertex[_, _] => Boolean)

case class CommandSetSignalThreshold(signalThreshold: Double)
case class CommandSetCollectThreshold(collectThreshold: Double)

case class CommandForEachVertex[U](f: (Vertex[_, _]) => U)
case class CommandAggregate[ValueType](neutralElement: ValueType, aggregator: (ValueType, ValueType) => ValueType, extractor: (Vertex[_, _]) => ValueType)
case class StatusAggregatedValue[ValueType](value: ValueType)

// vertex/edge counting
case class StatusNumberOfVertices(v: Long)
case class StatusNumberOfEdges(e: Long)

// synchronous control messages
case object CommandSignalStep
case object CommandCollectStep

// synchronous worker to coordinator messages
case object StatusSignalStepDone
case class StatusCollectStepDone(signalOperationsPending: Long)

// asynchronous worker to coordinator messages
case object StatusWorkerIsIdle
case object StatusWorkerIsBusy
case object StatusWorkerHasPaused

case class ComputationProgressStats(
  collectOperationsPending: Long = 0l,
  collectOperationsExecuted: Long = 0l,
  signalOperationsPending: Long = 0l,
  signalOperationsExecuted: Long = 0l,
  verticesAdded: Long = 0l,
  verticesRemoved: Long = 0l,
  outgoingEdgesAdded: Long = 0l,
  outgoingEdgesRemoved: Long = 0l,
  incomingEdgesAdded: Long = 0l) {
  def +(other: ComputationProgressStats) = {
    ComputationProgressStats(
      collectOperationsPending + other.collectOperationsPending,
      collectOperationsExecuted + other.collectOperationsExecuted,
      signalOperationsPending + other.signalOperationsPending,
      signalOperationsExecuted + other.signalOperationsExecuted,
      verticesAdded + other.verticesAdded,
      verticesRemoved + other.verticesRemoved,
      outgoingEdgesAdded + other.outgoingEdgesAdded,
      outgoingEdgesRemoved + other.outgoingEdgesRemoved,
      incomingEdgesAdded + other.incomingEdgesAdded)
  }
}

// asynchronous worker to coordinator messages
case object CommandSendComputationProgressStats
case object CommandPauseComputation
case object CommandStartComputation

// coordinator to worker/logger message
case object CommandShutDown