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

import signalcollect.implementations.coordinator.SynchronousCoordinator
import signalcollect.implementations.coordinator.AsynchronousCoordinator
import signalcollect.api.Factory._
import signalcollect.interfaces._
import signalcollect.interfaces.ComputeGraph._
import signalcollect.interfaces.MessageRecipient

/**
 * Default [[signalcollect.interfaces.ComputeGraph]] implementation.
 *
 * @param executionMode asynchronous or synchronous execution mode
 * @param numberOfWorkers number of [[signalcollect.interfaces.Worker]]s
 * @param workerFactory factory to create worker instances
 * @param messageBusFactory factory used to create message bus instances
 * @param logger optional: used to process logging messages
 *
 * Signal/Collect supports two execution modes: synchronous and asynchronous.
 * These modes are about the order in which the signal/collect operations
 * on vertices are scheduled. A synchronous execution guarantees that all
 * vertices execute the same operation at the same time. In a synchronous
 * execution it can never happen that one vertex executes a signal operation
 * while another vertex is executing a collect operation, because the switch
 * from one phase to the other is globally synchronized. In contrast, the
 * asynchronous mode does not guarantee this. Consequently, the asynchronous
 * mode allows for optimizations by means of scheduling strategies and
 * operation scoring in the [[signalcollect.interfaces.Worker]]
 * implementation.
 */
class DefaultComputeGraph(
  executionMode: ExecutionMode = AsynchronousExecutionMode,
  numberOfWorkers: Int = ComputeGraph.defaultNumberOfThreadsUsed,
  workerFactory: WorkerFactory = Factory.Worker.Default,
  messageBusFactory: MessageBusFactory = Factory.MessageBus.Default,
  storageFactory: StorageFactory = Factory.Storage.Default,
  optionalLogger: Option[MessageRecipient[Any]] = None,
  signalThreshold: Double = ComputeGraph.defaultSignalThreshold,
  collectThreshold: Double = ComputeGraph.defaultCollectThreshold) extends ComputeGraph {

  val coordinator = {
    executionMode match {
      case AsynchronousExecutionMode => new AsynchronousCoordinator(
        numberOfWorkers,
        workerFactory,
        messageBusFactory,
        storageFactory,
        optionalLogger,
        signalThreshold,
        collectThreshold)
      case SynchronousExecutionMode => new SynchronousCoordinator(
        numberOfWorkers,
        workerFactory,
        messageBusFactory,
        storageFactory,
        optionalLogger,
        signalThreshold,
        collectThreshold)
    }
  }

  /**
   * Forward ComputeGraph calls to coordinator
   */

  /**
   * Sends a signal to the vertex with vertex.id=targetId.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) {
    coordinator.sendSignalToVertex(signal, targetId, sourceId)
  }

  /**
   * Sends a signal to all vertices.
   * The senderId of this signal will be signalcollect.interfaces.External
   */
  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL) {
    coordinator.sendSignalToAllVertices(signal, sourceId)
  }
  
  
  def execute = coordinator.execute
  def shutDown = coordinator.shutDown

  def foreach(f: (Vertex[_, _]) => Unit) = coordinator.foreach(f)
  def foreach(f: PartialFunction[Vertex[_, _], Unit]) = coordinator.foreach(f)

  def countVertices[VertexType <: Vertex[_, _]](implicit m: Manifest[VertexType]): Long = {
    coordinator.countVertices[VertexType]
  }

  def sum[N](implicit numeric: Numeric[N]): N = coordinator.sum[N]
  def product[N](implicit numeric: Numeric[N]): N = coordinator.product[N]
  def reduce[ValueType](operation: (ValueType, ValueType) => ValueType): Option[ValueType] = {
    coordinator.reduce[ValueType](operation)
  }
  def aggregateStates[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType): ValueType = {
    coordinator.aggregateStates[ValueType](neutralElement, operation)
  }
  def customAggregate[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType,
    extractor: (Vertex[_, _]) => ValueType): ValueType = {
    coordinator.customAggregate[ValueType](neutralElement, operation, extractor)
  }

  def setUndeliverableSignalHandler(h: (Signal[_,_,_], GraphApi) => Unit) = coordinator.setUndeliverableSignalHandler(h)
  def setSignalThreshold(t: Double) = coordinator.setSignalThreshold(t)
  def setCollectThreshold(t: Double) = coordinator.setCollectThreshold(t)
  def setStepsLimit(l: Int) = coordinator.setStepsLimit(l)

  /**
   * Forward GraphApi calls to coordinator
   */

  /**
   * Adds a vertex with type VertexType.
   * The constructor is called with the parameter sequence: id, otherConstructorParameters.
   *
   * If a vertex with the same id already exists, then this operation is ignored.
   */
  def addVertex[VertexIdType](
    vertexClass: Class[_ <: Vertex[VertexIdType, _]],
    vertexId: VertexIdType,
    otherConstructorParameters: Any*) = {
    coordinator.addVertex[VertexIdType](vertexClass, vertexId, otherConstructorParameters: _*)
  }

  /**
   * Simpler alternative. Might have scalability/performance issues.
   */
  def addVertex(vertex: Vertex[_, _]) = coordinator.addVertex(vertex)
  
  /**
   * Adds an edge with type EdgeType.
   *
   * The constructor is called with the parameter sequence: sourceVertexId, targetVertexId, otherConstructorParameters.
   */
  def addEdge[SourceIdType, TargetIdType](
    edgeClass: Class[_ <: Edge[SourceIdType, TargetIdType]],
    sourceVertexId: SourceIdType,
    targetVertexId: TargetIdType,
    otherConstructorParameters: Any*) = {
    coordinator.addEdge(edgeClass, sourceVertexId, targetVertexId, otherConstructorParameters: _*)
  }

  /**
   * Simpler alternative. Might have scalability/performance issues.
   */
  def addEdge(edge: Edge[_, _]) = coordinator.addEdge(edge)

  
  def addPatternEdge[IdType, SourceVertexType <: Vertex[IdType, _]](
    sourceVertexPredicate: Vertex[IdType, _] => Boolean,
    edgeFactory: IdType => Edge[IdType, _]) = {
    coordinator.addPatternEdge(sourceVertexPredicate, edgeFactory)
  }

  def removeVertex(vertexId: Any) = coordinator.removeVertex(vertexId)

  def removeEdge(edgeId: (Any, Any, String)) = coordinator.removeEdge(edgeId)

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) = coordinator.removeVertices(shouldRemove)

}