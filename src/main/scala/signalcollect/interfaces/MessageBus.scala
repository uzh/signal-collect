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

trait MessageBus[MessageType, IdType] {
  def numberOfWorkers: Int
	
  def sendToWorkerForIdHash(m: MessageType, recipientIdHash: Int)
  def sendToWorkerForId(m: MessageType, recipientId: IdType)
  def sendToWorkers(m: MessageType)
  def sendToCoordinator(m: MessageType)
  def sendToLogger(m: Any)

  def registerWorker(id: Int, worker: MessageRecipient[MessageType])
  def registerCoordinator(coordinator: MessageRecipient[MessageType])
  def registerLogger(logger: MessageRecipient[Any]) 
}