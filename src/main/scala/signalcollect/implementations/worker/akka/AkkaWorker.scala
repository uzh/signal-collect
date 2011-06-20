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

package signalcollect.implementations.worker.akka

import akka.actor.Actor
import Actor._
import akka.dispatch.Dispatchers

import signalcollect.implementations.worker._

import signalcollect.implementations.messaging.AbstractMessageRecipient
import java.util.concurrent.TimeUnit
import signalcollect.implementations._
import signalcollect.interfaces._
import signalcollect.api.Factory._
import java.util.concurrent.BlockingQueue
import java.util.HashSet
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set
import signalcollect.interfaces.ALL
import signalcollect.implementations.graph.DefaultGraphApi
import signalcollect.implementations.storage.DefaultSerializer

//abstract class AkkaWorker(
//  workerId: Int,
//  mb: MessageBus[Any, Any],
//  messageInboxFactory: QueueFactory,
//  storageFactory: StorageFactory)
//  extends AbstractWorker(workerId, mb, messageInboxFactory, storageFactory)
//  with Actor {
//
//  /**
//   * Terminate the actor
//   */
//  override def processShutDownMessage = { 
//    self.stop()
//  }
//  
//  /**
//   * Checks if the Actor mailbox is empty 
//   */
//  def mailboxIsEmpty: Boolean = if (self == null) true else self.dispatcher.mailboxIsEmpty(self)
//    
//  def run = sys.error("Run should not be called from Akka Workers")
//  
//  override def initialize = sys.error("Initialize should not be called from Akka Worker but from the wrapper")
//  
//  override def receive(message: Any) = sys.error("Receive should not be called from Akka Workers. This receive is not the same one from Akka.")
//  
//}