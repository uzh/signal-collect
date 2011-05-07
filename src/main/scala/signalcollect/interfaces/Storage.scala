/*
 *  @author Daniel Strebel
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
 */

package signalcollect.interfaces

import signalcollect.implementations.serialization.{ DefaultStorage, InMemoryStorage, MongoDBToDoList, MongoDB, BerkDBJE, CachedDB }

object Storage {
  type StorageFactory = (MessageBus[Any, Any]) => Storage
  lazy val defaultFactory = inMemoryStorageFactory

  val inMemoryStorageFactory = new DefaultStorage(_)

  //Highly experimental
  //Use at your own risk!

  //Berkeley DB Storage (can be run directly from jar)
  class BerkeleyDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with BerkDBJE
  lazy val berkeleyDBStorageFactory = new BerkeleyDBStorage(_)
  
  //Berkeley DB Storage with InMemory caching
  class CachedStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with CachedDB
  lazy val cachedStorageFactory = new CachedStorage(_)

  //Mongo DB Storage (requires a running mongoDB installation)
  class MongoDBStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with MongoDB
  lazy val mongoDBStorageFactory = new MongoDBStorage(_)

  class AllOnDiskMongoDBStorage(messageBus: MessageBus[Any, Any]) extends MongoDBStorage(messageBus) with MongoDBToDoList
  lazy val allOnDiskMongoDBStorageFactory = new AllOnDiskMongoDBStorage(_)
  

  
}

abstract class Storage(messageBus: MessageBus[Any, Any]) {
  def getMessageBus = messageBus
  def vertices: VertexStore
  def toSignal: VertexIdSet
  def toCollect: VertexIdSet
}