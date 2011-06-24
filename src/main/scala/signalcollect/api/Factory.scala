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

object Factory {

  object Storage {

    object InMemory extends StorageFactory {
      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new DefaultStorage(messageBus)
    }

    //Berkeley DB Storage (can be run directly from jar)
    object BerkeleyDB extends StorageFactory {
      class BerkeleyDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with BerkDBJE
      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new BerkeleyDBStorage(messageBus)
    }

    //Berkeley DB Storage with InMemory caching
    object CachedBerkeleyDB extends StorageFactory {
      class CachedStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with ScoredCache
      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new CachedStorage(messageBus)
    }

//    //Mongo DB Storage (requires a running mongoDB installation)
//    object MongoDB extends StorageFactory {
//      class MongoDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with MongoDB
//      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new MongoDBStorage(messageBus)
//    }
//
//    //Mongo DB Storage that also stores all toSignal/toCollect lists on disk
//    object AllOnDiskMongoDB extends StorageFactory {
//      class MongoDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with MongoDB
//      class AllOnDiskMongoDBStorage(messageBus: MessageBus[Any, Any]) extends MongoDBStorage(messageBus) with MongoDBToDoList
//      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new AllOnDiskMongoDBStorage(messageBus)
//    }
//
//    //Orient DB Storage (can be run directly from jar, pure java)
//    object OrientDB extends StorageFactory {
//      class OrientDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with Orient
//      def createInstance(messageBus: MessageBus[Any, Any]): Storage = new OrientDBStorage(messageBus)
//    }

  }

  object MessageBus {
    object SharedMemory extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any, Any] = new DefaultMessageBus[Any, Any](numberOfWorkers, mapper)
    }

    object Verbose extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any, Any] = new DefaultMessageBus[Any, Any](numberOfWorkers, mapper) with Verbosity[Any, Any]
    }
  }

  object Worker {
    object Asynchronous extends WorkerFactory {
      class AsynchronousWorker(workerId: Int,
        messageBus: MessageBus[Any, Any],
        storageFactory: StorageFactory)
        extends AbstractWorker(workerId,
          messageBus,
          storageFactory) with AsynchronousExecution
      def createInstance(workerId: Int, messageBus: MessageBus[Any, Any], storageFactory: StorageFactory): Worker = new AsynchronousWorker(workerId, messageBus, storageFactory)
    }

    object Synchronous extends WorkerFactory {
      class SynchronousWorker(workerId: Int,
        messageBus: MessageBus[Any, Any],
        storageFactory: StorageFactory)
        extends AbstractWorker(workerId,
          messageBus,
          storageFactory) with SynchronousExecution
      def createInstance(workerId: Int, messageBus: MessageBus[Any, Any], storageFactory: StorageFactory): Worker = new SynchronousWorker(workerId, messageBus, storageFactory)
    }
    
    
//    lazy val BufferingSynchronous: WorkerFactory = new SynchronousWorker(_, _, Queue.Default, _) with SignalBuffer
//    lazy val BufferingAsynchronous: WorkerFactory = new AsynchronousWorker(_, _, Queue.Default, _) with SignalBuffer
  //    lazy val AkkaSynchronous: WorkerFactory = { (workerId: Int, mb: MessageBus[Any, Any], sf: StorageFactory) => new ActorRefAdapter("AkkaSynchronousWorker", actorOf(new AkkaSynchronousWorker(workerId, mb, Queue.Default, sf)) ) }
  //    lazy val AkkaAsynchronous: WorkerFactory = { (workerId: Int, mb: MessageBus[Any, Any], sf: StorageFactory) => new ActorRefAdapter("AkkaAsynchronousWorker", actorOf(new AkkaAsynchronousWorker(workerId, mb, Queue.Default, sf)) ) }

    
  }

  //  /**
  //   * Wrapper class for the actor reference to be kept
  //   */
  //  class ActorRefAdapter(actorType: String, actorRef: ActorRef) extends Worker {
  //
  //    /**
  //     * used for getting the correct type of actor
  //     */
  //    override def toString = actorType
  //
  //    def receive(message: Any) = actorRef ! message
  //
  //    def initialize = {
  //      actorRef.dispatcher = Dispatchers.newThreadBasedDispatcher(actorRef)
  //      actorRef.start()
  //    }
  //
  //  }

}