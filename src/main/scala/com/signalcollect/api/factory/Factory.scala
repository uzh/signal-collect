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

package com.signalcollect.api

import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import com.signalcollect.implementations.worker._
import com.signalcollect.implementations.messaging._
import com.signalcollect.implementations.storage._
import com.signalcollect.implementations.coordinator.WorkerApi

import akka.actor.Actor._
import akka.actor.ActorRef

import java.util.concurrent.LinkedBlockingQueue

package factory {

  package storage {

    object InMemory extends StorageFactory {
      def createInstance(messageBus: MessageBus[Any]): Storage = new DefaultStorage(messageBus)
    }

    //Berkeley DB Storage (can be run directly from jar)
    object BerkeleyDB extends StorageFactory {
      class BerkeleyDBStorage(messageBus: MessageBus[Any]) extends DefaultStorage(messageBus) with BerkDBJE
      def createInstance(messageBus: MessageBus[Any]): Storage = new BerkeleyDBStorage(messageBus)
    }

    //Berkeley DB Storage with InMemory caching
    object CachedBerkeleyDB extends StorageFactory {
      class CachedStorage(messageBus: MessageBus[Any]) extends DefaultStorage(messageBus) with ScoredCache
      def createInstance(messageBus: MessageBus[Any]): Storage = new CachedStorage(messageBus)
    }

  }

  package messageBus {
    object SharedMemory extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new DefaultMessageBus[Any](numberOfWorkers, mapper)
    }
    
    object AkkaBus extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new AkkaMessageBus[Any](numberOfWorkers, mapper)
    }
    
  }

  package worker {

    object Local extends LocalWorkerFactory {
      def createInstance(workerId: Int,
        config: Configuration,
        coordinator: WorkerApi,
        mapper: VertexToWorkerMapper): Worker = new LocalWorker(workerId, config, coordinator, mapper)
    }

    object BufferedLocal extends LocalWorkerFactory {
      def createInstance(workerId: Int,
        config: Configuration,
        coordinator: WorkerApi,
        mapper: VertexToWorkerMapper): Worker = new LocalWorker(workerId, config, coordinator, mapper) with SignalBuffer
    }
    
    object AkkaLocal extends AkkaWorkerFactory {
      def createInstance(workerId: Int,
        config: Configuration,
        coordinator: WorkerApi,
        mapper: VertexToWorkerMapper): ActorRef = actorOf(new AkkaWorker(workerId, config, coordinator, mapper))
      }
    
  }

}