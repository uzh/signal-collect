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

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.factory.storage.DefaultStorage
import com.signalcollect.factory.storage.JavaMapStorage
import com.signalcollect.messaging.DefaultMessageBus
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VertexStorageSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "Custom Vertex Storage" should {

    val storageFactory = DefaultStorage

    "hold all vertices inserted" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.vertices.size must_== vertexList.size
    }

    "not add vertices automatically to the toSignal list" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.toSignal.size must_== 0
    }

    "not add vertices automatically to the toCollect list" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.toCollect.size must_== 0
    }

    "remove vertices from the store" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.vertices.remove(0)
      storage.vertices.size must_== vertexList.size - 1
    }
  }

  "Java Map Vertex Storage" should {

    val storageFactory = JavaMapStorage

    "hold all vertices inserted" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.vertices.size must_== vertexList.size
    }

    "not add vertices automatically to the toSignal list" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.toSignal.size must_== 0
    }

    "not add vertices automatically to the toCollect list" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.toCollect.size must_== 0
    }

    "remove vertices from the store" in {
      val defaultMessageBus = mock[DefaultMessageBus[Any, Any]]
      val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
      val storage = storageFactory.createInstance[Any]
      vertexList.foreach(storage.vertices.put(_))
      storage.vertices.remove(0)
      storage.vertices.size must_== vertexList.size - 1
    }
  }

}