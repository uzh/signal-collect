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
import signalcollect.implementations.worker.akka._
import java.util.concurrent.TimeUnit._
import signalcollect.interfaces._
import signalcollect.implementations.worker._
import signalcollect.implementations.messaging._
import signalcollect.implementations.storage._
import java.util.concurrent.LinkedBlockingQueue
import signalcollect.configuration._

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
    
    
    object Local extends WorkerFactory {
      def createInstance(workerId: Int, workerConfiguration: WorkerConfiguration): Worker = new LocalWorker(workerId, workerConfiguration: WorkerConfiguration)
    }

    /*object Synchronous extends WorkerFactory {
      class SynchronousWorker(workerId: Int,
        workerConfiguration: WorkerConfiguration)
        extends AbstractWorker(workerId,
          workerConfiguration) with SynchronousExecution
      def createInstance(workerId: Int, workerConfiguration: WorkerConfiguration): Worker = new SynchronousWorker(workerId, workerConfiguration: WorkerConfiguration)
    }*/

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