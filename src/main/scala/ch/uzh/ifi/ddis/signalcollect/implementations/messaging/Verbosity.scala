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
import ch.uzh.ifi.ddis.signalcollect.interfaces._

trait Verbosity[MessageType, IdType] extends MessageBus[MessageType, IdType] with Logging {
  protected def messageBus = this
  
  abstract override def sendToCoordinator(m: MessageType) {
	  sendToLogger(Thread.currentThread.getName + "=>Coordinator:\t" + m)
	  super.sendToCoordinator(m)
  }
  
  abstract override def sendToWorkerForIdHash(m: MessageType, recipientIdHash: Int) = {
	  val workerNumber = (recipientIdHash % numberOfWorkers).abs
	  sendToLogger(Thread.currentThread.getName + "=>Worker#" + workerNumber + ":\t" + m)
	  super.sendToWorkerForIdHash(m, recipientIdHash)
  }

  abstract override def sendToWorkerForId(m: MessageType, recipientId: IdType) {
	  val workerNumber = (recipientId.hashCode % numberOfWorkers).abs
	  sendToLogger(Thread.currentThread.getName + "=>Worker#" + workerNumber + ":\t" + m)
	  super.sendToWorkerForId(m, recipientId)
  }

  abstract override def sendToWorkers(m: MessageType) {
	  sendToLogger(Thread.currentThread.getName + "=>Workers:\t" + m)
	  super.sendToWorkers(m)
  }
}