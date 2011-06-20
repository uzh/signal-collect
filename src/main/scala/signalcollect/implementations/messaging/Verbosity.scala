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

package signalcollect.implementations.messaging

import signalcollect._
import signalcollect.interfaces._

trait Verbosity[MessageType, IdType] extends MessageBus[MessageType, IdType] with Logging {
  protected def messageBus = this

  abstract override def sendToCoordinator(m: MessageType) {
    sendToLogger(Thread.currentThread.getName + "=>Coordinator:\t" + m)
    super.sendToCoordinator(m)
  }

  override abstract def sendToWorkerForVertexIdHash(m: MessageType, recipientIdHash: Int) {
    val workerId = (recipientIdHash % numberOfWorkers).abs
    sendToLogger(Thread.currentThread.getName + "=>Worker" + workerId + ":\t" + m)
    super.sendToWorkerForVertexIdHash(m, recipientIdHash)
  }

  override abstract def sendToWorkerForVertexId(m: MessageType, recipientId: IdType) {
    val workerId = (recipientId.hashCode % numberOfWorkers).abs
    sendToLogger(Thread.currentThread.getName + "=>Worker" + workerId + ":\t" + m)
    super.sendToWorkerForVertexId(m, recipientId)
  }

  abstract override def sendToWorker(workerId: Int, m: MessageType) {
    sendToLogger(Thread.currentThread.getName + "=>Worker" + workerId + ":\t" + m)
    super.sendToWorker(workerId, m)
  }

  abstract override def sendToWorkers(m: MessageType) {
    sendToLogger(Thread.currentThread.getName + "=>AllWorkers:\t" + m)
    super.sendToWorkers(m)
  }
}