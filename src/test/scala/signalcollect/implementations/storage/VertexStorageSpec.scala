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
 *  
 */

package signalcollect.implementations.storage

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import signalcollect.interfaces._
import signalcollect.implementations.messaging.DefaultMessageBus
import signalcollect.algorithms.Page
import java.io.File

@RunWith(classOf[JUnitRunner])
class VertexStorageSpec extends SpecificationWithJUnit with Mockito {

  /**
   * Check for read/write permission on temporary folder
   */
  def hasReadAndWritePermission(path: String): Boolean = {
    val tempFolder = new File(path)
    tempFolder.canWrite && tempFolder.canRead
  }

  "InMemory Vertex Store" should {
    val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
    val vertexList = List(new Page(0, 1), new Page(1, 1), new Page(2, 1))
    val inMemoryStore = new DefaultStorage(defaultMessageBus)
    vertexList.foreach(v => inMemoryStore.vertices.put(v))

    "hold all vertices inserted" in {
      inMemoryStore.vertices.size must_== vertexList.size
    }

    "add all added vertices to the toSignal list" in {
      inMemoryStore.toSignal.size must_== vertexList.size
    }

    "add all added vertices to the toCollect list" in {
      inMemoryStore.toCollect.size must_== vertexList.size
    }

    "remove vertices from the store" in {
      val inMemoryStore = new DefaultStorage(defaultMessageBus)
      vertexList.foreach(v => inMemoryStore.vertices.put(v))
      inMemoryStore.vertices.remove(0)
      inMemoryStore.vertices.size must_== vertexList.size - 1
    }
  }

  "Berkeley DB Vertex Store" should {
    if (hasReadAndWritePermission("/tmp/")) {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new Page(0, 0.5), new Page(1, 0.5), new Page(2, 0.5))
      class BerkeleyStorage(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with BerkDBJE
      val berkeleyStore = new BerkeleyStorage(defaultMessageBus)
      vertexList.foreach(v => berkeleyStore.vertices.put(v))

      "hold all vertices inserted" in {
        berkeleyStore.vertices.size must_== vertexList.size
      }

      "add all added vertices to the toSignal list" in {
        berkeleyStore.toSignal.size must_== vertexList.size
      }

      "add all added vertices to the toCollect list" in {
        berkeleyStore.toCollect.size must_== vertexList.size
      }

      "reflect changes on a vertex" in {
        val berkeleyStore = new BerkeleyStorage(defaultMessageBus)
        val vertex0 = new Page(0, 0)
        val state0 = vertex0.state
        berkeleyStore.vertices.put(vertex0)
        val vertex1 = berkeleyStore.vertices.get(0)
        val state1 = vertex1.state
        berkeleyStore.vertices.updateStateOfVertex(new Page(0, 1)) //simulates an update of the vertex since the id (1st parameter) remains unchanged.
        val state2 = berkeleyStore.vertices.get(0).state
        state0 must_== state1
        state1 must_!= state2
      }

      "remove vertices from the store" in {
        val berkeleyStore = new BerkeleyStorage(defaultMessageBus)
        vertexList.foreach(v => berkeleyStore.vertices.put(v))
        berkeleyStore.vertices.remove(0)
        berkeleyStore.vertices.size must_== vertexList.size - 1
      }

      "create a directory" in {
        val directoryPath = "/tmp/" + "testdir"
        trait BerkDBJE2 extends DefaultStorage {
          override protected def vertexStoreFactory = new BerkeleyDBStorage(this, directoryPath)
        }
        class BerkeleyStorage2(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with BerkDBJE2
        val berkeleyStore = new BerkeleyStorage2(defaultMessageBus) // This should create the folder with path directoryPath if not existent.
        directoryPath must beADirectoryPath
      }
    } else { //No permission in /temp folder
      "fail gracefully because no write permissions for temp folder exist" in {
        1 === 1
      }
    }
  }

  "LRU cached Berkeley DB" should {

    if (hasReadAndWritePermission("/tmp/")) {

      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new Page(0, 1), new Page(1, 1), new Page(2, 1))
      class CachedBerkeley(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with LRUCache
      val cachedStore = new CachedBerkeley(defaultMessageBus)
      vertexList.foreach(v => cachedStore.vertices.put(v))

      "hold all vertices inserted" in {
        cachedStore.vertices.size must_== vertexList.size
      }

      "add all added vertices to the toSignal list" in {
        cachedStore.toSignal.size must_== vertexList.size
      }

      "add all added vertices to the toCollect list" in {
        cachedStore.toCollect.size must_== vertexList.size
      }
    } else { //No permission in /tmp folder
      "fail gracefully because no write permissions for temp folder exist" in {
        1 === 1
      }
    }
  }

  "OrientDB" should {
    if (hasReadAndWritePermission("/tmp/")) {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new Page(0, 0.1), new Page(1, 0.1), new Page(2, 0.1))
      class OrientDB(messageBus: MessageBus[Any, Any]) extends DefaultStorage(messageBus) with Orient
      val orientStore = new OrientDB(defaultMessageBus)
      vertexList.foreach(v => orientStore.vertices.put(v))

      "Hold all inserted Vertices" in {
        vertexList.size === orientStore.vertices.size
      }

      "add all added vertices to the toSignal list" in {
        orientStore.toSignal.size must_== vertexList.size
      }

      "add all added vertices to the toCollect list" in {
        orientStore.toCollect.size must_== vertexList.size
      }

      "retreive an added vertex" in {
        orientStore.vertices.get(1).hashCode === vertexList(1).hashCode
      }

      "reflect Changes" in {
        val v1_old: Vertex[_, _] = orientStore.vertices.get(1)
        var v1_changed = new Page(1, 0.9)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.8)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.7)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.6)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.5)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        v1_changed = new Page(1, 0.4)
        orientStore.vertices.updateStateOfVertex(v1_changed)
        val v1_new = orientStore.vertices.get(1)
        if (v1_old != v1_changed) {
          v1_new.state must_!= v1_old.state //Entries returned differ
        }
        v1_new.state === v1_changed.state
        orientStore.vertices.size === 3l //old entry is replaced
      }

      "delete a vertex" in {
        orientStore.vertices.remove(1)
        orientStore.vertices.size === 2l //old entry is replaced

      }
    } else { //No permission in /tmp folder
      "fail gracefully because no write permissions for temp folder exist" in {
        1 === 1
      }
    }
  }
}