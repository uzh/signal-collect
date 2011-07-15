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
import java.util.HashMap
import signalcollect.implementations.logging.DefaultLogger
import signalcollect.interfaces.LogMessage

class DefaultMessageBus[IdType](
  val numberOfWorkers: Int,
  protected val mapper: VertexToWorkerMapper)
  extends MessageBus[IdType] {

  protected val workers = new Array[MessageRecipient[Any]](numberOfWorkers)
  protected var coordinator: MessageRecipient[Any] = _

  var messagesSent = 0l

  def registerWorker(workerId: Int, w: MessageRecipient[Any]) {
    workers(workerId) = w
  }

  def registerCoordinator(c: MessageRecipient[Any]) {
    coordinator = c
  }

  def sendToCoordinator(message: Any) {
    if (!message.isInstanceOf[LogMessage]) {
      messagesSent += 1
    }
    coordinator.receive(message)
  }

  def sendToWorkerForVertexId(message: Any, recipientId: IdType) {
    val worker = workers(mapper.getWorkerIdForVertexId(recipientId))
    messagesSent += 1
    worker.receive(message)
  }

  def sendToWorkerForVertexIdHash(message: Any, recipientIdHash: Int) {
    val worker = workers(mapper.getWorkerIdForVertexIdHash(recipientIdHash))
    messagesSent += 1
    worker.receive(message)
  }

  def sendToWorker(workerId: Int, m: Any) {
    messagesSent += 1
    workers(workerId).receive(m)
  }

  def sendToWorkers(message: Any) {
    messagesSent += numberOfWorkers
    val i = workers.iterator
    while (i.hasNext) {
      val worker = i.next
      worker.receive(message)
    }
  }
}