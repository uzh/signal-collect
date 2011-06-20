/*
 *  @author Philip Stutz
 *  
 *  Copyright 2011 University of Zurich
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

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import akka.util.Duration
import akka.dispatch.Dispatchers
import akka.dispatch.Dispatchers._
import akka.dispatch.MessageDispatcher
import akka.dispatch.UnboundedMailbox
import akka.dispatch.UnboundedMessageQueueSemantics
import akka.dispatch.ExecutableMailbox
import akka.dispatch.MessageInvocation
import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import signalcollect.implementations.worker.akka._
import java.util.concurrent.TimeUnit._
import signalcollect.interfaces._
import signalcollect.implementations.worker._
import signalcollect.implementations.messaging._
import signalcollect.implementations.storage._
import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.forkjoin.LinkedTransferQueue
import java.util.concurrent.BlockingQueue

object Factory {
  type StorageFactory = (MessageBus[Any, Any]) => Storage
  type MessageBusFactory = () => MessageBus[Any, Any]
  type WorkerFactory = (MessageBus[Any, Any], StorageFactory) => Worker
  type QueueFactory = () => BlockingQueue[Any]

  object Storage {
    lazy val Default: StorageFactory = InMemory
    lazy val InMemory: StorageFactory = new DefaultStorage(_)
    
    //==================================================
    //Highly experimental
    //Use at your own risk!
    //==================================================
    
    //Berkeley DB Storage (can be run directly from jar)
    class BerkeleyDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with BerkDBJE
    lazy val BerkeleyDB: StorageFactory = new BerkeleyDBStorage(_)
    
    //Berkeley DB Storage with InMemory caching
    class CachedStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with ScoredCache
    lazy val Cached: StorageFactory = new CachedStorage(_)
    
    //Mongo DB Storage (requires a running mongoDB installation)
    class MongoDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with MongoDB
    lazy val MongoDB: StorageFactory = new MongoDBStorage(_)
    
    //Mongo DB Storage that also stores all toSignal/toCollect lists on disk
    class AllOnDiskMongoDBStorage(messageBus: MessageBus[Any, Any]) extends MongoDBStorage(messageBus) with MongoDBToDoList
    lazy val AllOnDiskMongoDB: StorageFactory = new AllOnDiskMongoDBStorage(_)
    
    //Orient DB Storage (can be run directly from jar, pure java)
    class OrientDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with Orient
    lazy val OrientDB: StorageFactory = new OrientDBStorage(_)
  }

  object MessageBus {
    lazy val Default: MessageBusFactory = SharedMemory
    lazy val SharedMemory: MessageBusFactory = () => new DefaultMessageBus[Any, Any]
    lazy val Verbose: MessageBusFactory = () => new DefaultMessageBus[Any, Any] with Verbosity[Any, Any]
  }

  object Worker {
    lazy val Default: WorkerFactory = Asynchronous
    lazy val Synchronous: WorkerFactory = new SynchronousWorker(_, Queue.Default, _)
    lazy val Asynchronous: WorkerFactory = new AsynchronousWorker(_, Queue.Default, _)

    lazy val AkkaSynchronous: WorkerFactory = { (mb: MessageBus[Any, Any], sf: StorageFactory) => new ActorRefAdapter("AkkaSynchronousWorker", actorOf(new AkkaSynchronousWorker(mb, Queue.Default, sf)) ) }
    lazy val AkkaAsynchronous: WorkerFactory = { (mb: MessageBus[Any, Any], sf: StorageFactory) => new ActorRefAdapter("AkkaAsynchronousWorker", actorOf(new AkkaAsynchronousWorker(mb, Queue.Default, sf)) ) }

  }

  /**
   * Wrapper class for the actor reference to be kept
   */
  class ActorRefAdapter(actorType: String, actorRef: ActorRef) extends Worker {

    /**
     * used for getting the correct type of actor
     */
    override def toString = actorType

    def receive(message: Any) = actorRef ! message

    def initialize = {
      actorRef.dispatcher = Dispatchers.newThreadBasedDispatcher(actorRef)
      actorRef.start()
    }

  }
  object Queue {
    lazy val Default: QueueFactory = LinkedTransfer
    lazy val LinkedTransfer: QueueFactory = () => new LinkedTransferQueue[Any]
    lazy val LinkedBlocking: QueueFactory = () => new LinkedBlockingQueue[Any]
  }
}