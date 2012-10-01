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

import akka.dispatch.Future
import akka.actor.ActorRef
import com.signalcollect.GraphEditor
import java.util.concurrent.atomic.AtomicInteger
import com.signalcollect.coordinator.WorkerApi

/**
 *  A message bus is responsible for sending messages.
 *  It has to guarantee per-sender FIFO when delivering messages.
 */
trait MessageBus extends MessageRecipientRegistry with VertexToWorkerMapper {
  def isInitialized: Boolean
  
  def numberOfWorkers: Int

  def messagesSent: Array[Int]
  def messagesReceived: Long

  def getReceivedMessagesCounter: AtomicInteger

  def sendToActor(actor: ActorRef, m: Any) 
  
  def sendToLogger(m: LogMessage)

  def sendToWorkerForVertexIdHash(m: Any, vertexIdHash: Int)

  def sendToWorkerForVertexId(m: Any, vertexId: Any)

  def sendToWorker(workerId: Int, m: Any)

  def sendToWorkers(m: Any)

  def sendToCoordinator(m: Any)

  // Returns an api that treats all workers as if there were only one.
  def getWorkerApi: WorkerApi  //TODO remove dependency on coordinator package.

  // Returns an array of worker proxies for all workers, indexed by workerId.
  def getWorkerProxies: Array[Worker]

  // Returns a graph editor that allows to manipulate the graph.
  def getGraphEditor: GraphEditor
}

trait MessageRecipientRegistry {
  
  /**
   *  Registers a worker.
   *
   *  @param workerId is the worker id
   *  @param worker is the worker to be registered
   */
  def registerWorker(workerId: Int, worker: ActorRef)

  /**
   *  Registers a coordinator.
   *
   *  @param coordinator is the coordinator to be registered
   */
  def registerCoordinator(coordinator: ActorRef)

  /**
   *  Registers a logger.
   *
   *  @param logger is the logger to be registered
   */
  def registerLogger(logger: ActorRef)
}