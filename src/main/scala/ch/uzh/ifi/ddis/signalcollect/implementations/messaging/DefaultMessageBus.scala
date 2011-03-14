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

package ch.uzh.ifi.ddis.signalcollect.implementations.messaging

import ch.uzh.ifi.ddis.signalcollect._
import java.util.HashMap

class DefaultMessageBus[MessageType, IdType] extends interfaces.MessageBus[MessageType, IdType] {

  var coordinator: interfaces.MessageRecipient[MessageType] = _
  var logger: Option[interfaces.MessageRecipient[Any]] = None
  val workers = new HashMap[Int, interfaces.MessageRecipient[MessageType]]

  def registerWorker(id: Int, worker: interfaces.MessageRecipient[MessageType]) {
    workers.put(id, worker)
    numberOfWorkers = workers.size
  }

  def registerCoordinator(c: interfaces.MessageRecipient[MessageType]) {
    coordinator = c
  }

  def registerLogger(l: interfaces.MessageRecipient[Any]) {
    logger = Some(l)
  }
  
  var numberOfWorkers = 0

  def sendToCoordinator(message: MessageType) {
    coordinator.send(message)
  }
  
  def sendToLogger(message: Any) {
    logger foreach (_.send(message))
  }

  def sendToWorkerForId(message: MessageType, recipientId: IdType) {
    val worker = workers.get((recipientId.hashCode % numberOfWorkers).abs)
    worker.send(message)
  }

  def sendToWorkerForIdHash(message: MessageType, recipientIdHash: Int) {
    val worker = workers.get((recipientIdHash % numberOfWorkers).abs)
    worker.send(message)
  }
  
  def sendToWorkers(message: MessageType) {
	val i = workers.values.iterator
	while (i.hasNext) {
		val worker = i.next
		worker.send(message)
	}
  }
}