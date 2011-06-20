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

import signalcollect.api.Factory._
import signalcollect.interfaces._
import signalcollect.interfaces.ComputeGraph._
import signalcollect.interfaces.MessageRecipient
import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.implementations.coordinator._
import signalcollect.implementations.messaging.DefaultVertexToWorkerMapper

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
  collectThreshold: Double = ComputeGraph.defaultCollectThreshold) extends ComputeGraph with GraphApi {

  val workerApi = new WorkerApi(
    numberOfWorkers = numberOfWorkers,
    messageBusFactory = messageBusFactory,
    workerFactory = workerFactory,
    storageFactory = storageFactory)

  protected val coordinator = {
    var configrationMap = Map[String, Any]()
    configrationMap += (("signalThreshold", signalThreshold))
    configrationMap += (("collectThreshold", collectThreshold))
    configrationMap += (("numberOfWorkers", numberOfWorkers))
    configrationMap += (("executionMode", executionMode.toString))
    configrationMap += (("storage", storageName))
    configrationMap += (("worker", workerName))
    configrationMap += (("messageBus", messageBusName))
    configrationMap += (("loggerName", loggerName))

    def workerName = workerFactory(0, messageBusFactory(1, new DefaultVertexToWorkerMapper(1)), storageFactory).toString // hack to get akka worker types
    def messageBusName = messageBusFactory(1, new DefaultVertexToWorkerMapper(1)).getClass.getSimpleName
    def storageName = storageFactory(messageBusFactory(1, new DefaultVertexToWorkerMapper(1))).getClass.toString.split('.').last.split('$').last
    def loggerName = optionalLogger.getClass.getSimpleName

    if (optionalLogger.isDefined) {
      workerApi.registerLogger(optionalLogger.get)
    }

    executionMode match {
      case AsynchronousExecutionMode => new AsynchronousCoordinator(workerApi, configrationMap)
      case SynchronousExecutionMode => new SynchronousCoordinator(workerApi, configrationMap)
    }
  }

  /** GraphApi */

  def execute: ComputationStatistics = coordinator.execute

  def setStepsLimit(l: Int) = coordinator.setStepsLimit(l)

  /** WorkerApi */

  def recalculateScores = workerApi.recalculateScores

  def recalculateScoresForVertexId(vertexId: Any) = workerApi.recalculateScoresForVertexId(vertexId)

  def shutdown = workerApi.shutdown

  def forVertexWithId(vertexId: Any, f: (Vertex[_, _]) => Unit) = workerApi.forVertexWithId(vertexId, f)

  def foreachVertex(f: (Vertex[_, _]) => Unit) = workerApi.foreachVertex(f)

  def customAggregate[ValueType](
    neutralElement: ValueType,
    operation: (ValueType, ValueType) => ValueType,
    extractor: (Vertex[_, _]) => ValueType): ValueType = {
    workerApi.customAggregate(neutralElement, operation, extractor)
  }

  def setSignalThreshold(t: Double) = workerApi.setSignalThreshold(t)

  def setCollectThreshold(t: Double) = workerApi.setCollectThreshold(t)

  def setUndeliverableSignalHandler(h: (Signal[_, _, _], GraphApi) => Unit) = workerApi.setUndeliverableSignalHandler(h)

  /** GraphApi */

  def sendSignalToVertex(signal: Any, targetId: Any, sourceId: Any = EXTERNAL) = workerApi.sendSignalToVertex(signal, sourceId)

  def sendSignalToAllVertices(signal: Any, sourceId: Any = EXTERNAL) = workerApi.sendSignalToAllVertices(signal, sourceId)

  def add(vertex: Vertex[_, _]) = workerApi.add(vertex)

  def add(edge: Edge[_, _]) = workerApi.add(edge)

  def addPatternEdge(sourceVertexPredicate: Vertex[_, _] => Boolean, edgeFactory: Vertex[_, _] => Edge[_, _]) {
    workerApi.addPatternEdge(sourceVertexPredicate, edgeFactory)
  }

  def removeVertex(vertexId: Any) = workerApi.removeVertex(vertexId)

  def removeEdge(edgeId: (Any, Any, String)) = workerApi.removeEdge(edgeId)

  def removeVertices(shouldRemove: Vertex[_, _] => Boolean) = workerApi.removeVertices(shouldRemove)

}