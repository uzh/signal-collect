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

import java.util.concurrent.LinkedBlockingQueue

package factory {

  package storage {

    object InMemory extends StorageFactory {
      def createInstance: Storage = new DefaultStorage
    }

    //Berkeley DB Storage (can be run directly from jar)
    object BerkeleyDB extends StorageFactory {
      class BerkeleyDBStorage extends DefaultStorage with BerkDBJE
      def createInstance: Storage = new BerkeleyDBStorage
    }

    //Berkeley DB Storage that uses ZLIB compression functionality to reduce the size of the serialized vertices
    object CompressedBerkeleyDB extends StorageFactory {
      class BerkeleyDBStorage extends DefaultStorage with CompressedSerialization with BerkDBJE
      def createInstance: Storage = new BerkeleyDBStorage
    }

    //Berkeley DB Storage with InMemory caching
    object CachedBerkeleyDB extends StorageFactory {
      class CachedStorage extends DefaultStorage with ScoredCache
      def createInstance: Storage = new CachedStorage
    }

  }

  package messageBus {
    object SharedMemory extends MessageBusFactory {
      def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any] = new DefaultMessageBus[Any](numberOfWorkers, mapper)
    }

  }

  package worker {

    object Local extends LocalWorkerFactory {
      def createInstance(workerId: Int,
                         workerConfig: WorkerConfiguration,
                         numberOfWorkers: Int,
                         coordinator: Any,
                         mapper: VertexToWorkerMapper): Worker = new LocalWorker(workerId, workerConfig, numberOfWorkers, coordinator, mapper)
    }

  }

}