/*
 *  @author Daniel Strebel
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

package com.signalcollect.storage

import org.scalatest.{Finders, FlatSpec, Matchers}

import com.signalcollect.examples.PageRankVertex
import com.signalcollect.factory.storage.{JavaMapStorage, MemoryEfficientStorage}
import com.signalcollect.util.TestAnnouncements

class VertexStorageSpec extends FlatSpec with Matchers with TestAnnouncements {

  val memoryEfficientStorage = new MemoryEfficientStorage[Any, Any]

  "Memory Efficient Vertex Storage" should "hold all vertices inserted" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = memoryEfficientStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.vertices.size shouldBe vertexList.size
  }

  it should "not add vertices automatically to the toSignal list" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = memoryEfficientStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.toSignal.size shouldBe 0
  }

  it should "not add vertices automatically to the toCollect list" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = memoryEfficientStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.toCollect.size shouldBe 0
  }

  it should "remove vertices from the store" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = memoryEfficientStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.vertices.remove(0)
    storage.vertices.size shouldBe vertexList.size - 1
  }

  val javaStorage = new JavaMapStorage[Any, Any]

  "Java Map Vertex Storage" should "hold all vertices inserted" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = javaStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.vertices.size shouldBe vertexList.size
  }

  it should "not add vertices automatically to the toSignal list" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = javaStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.toSignal.size shouldBe 0
  }

  it should "not add vertices automatically to the toCollect list" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = javaStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.toCollect.size shouldBe 0
  }

  it should "remove vertices from the store" in {
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val storage = javaStorage.createInstance
    vertexList.foreach(storage.vertices.put(_))
    storage.vertices.remove(0)
    storage.vertices.size shouldBe vertexList.size - 1
  }

}
