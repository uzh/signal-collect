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

/**
 *  A message bus is responsible for sending messages.
 *  It has to guarantee per-sender FIFO when delivering messages.
 */
trait MessageBus[IdType] extends MessageRecipientRegistry {
  def numberOfWorkers: Int

  def messagesSent: Long

  def sendToWorkerForVertexIdHash(m: Any, recipientIdHash: Int)
  def sendToWorkerForVertexId(m: Any, recipientId: IdType)
  def sendToWorker(workerId: Int, m: Any)
  def sendToWorkers(m: Any)

  def sendToCoordinator(m: Any)
}

trait MessageRecipientRegistry {
  /**
   *  Interface now is more generalized to accept any kind of worker (for the Akka case which should be an Actor Ref)
   *
   *  @param workerId is the worker id
   *  @param worker is the worker to be registered
   */
  def registerWorker(workerId: Int, worker: Any)

  /**
   *  Generalization of coordinator to accept Akka Forwarders to coordinator
   *
   *  @param coordinator is the coordinator to be registered
   */
  def registerCoordinator(coordinator: Any)
}