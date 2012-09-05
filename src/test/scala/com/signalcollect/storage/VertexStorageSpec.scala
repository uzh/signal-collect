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

package com.signalcollect.storage

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import com.signalcollect.messaging.DefaultMessageBus
import com.signalcollect.examples.PageRankVertex
import java.io.File

@RunWith(classOf[JUnitRunner])
class VertexStorageSpec extends SpecificationWithJUnit with Mockito {

  /**
   * Check for read/write permission on current folder
   */
  def hasReadAndWritePermission(path: String): Boolean = {
    val tempFolder = new File(path)
    tempFolder.canWrite && tempFolder.canRead
  }

  "InMemory Vertex Store" should {
    val defaultMessageBus = mock[DefaultMessageBus]
    val vertexList = List(new PageRankVertex(0, 1), new PageRankVertex(1, 1), new PageRankVertex(2, 1))
    val inMemoryStore = new DefaultStorage
    vertexList.foreach(v => inMemoryStore.vertices.put(v))

    "hold all vertices inserted" in {
      inMemoryStore.vertices.size must_== vertexList.size
    }

    "don't add vertices automatically to the toSignal list" in {
      inMemoryStore.toSignal.size must_== 0
    }

    "don't add vertices automatically to the toCollect list" in {
      inMemoryStore.toCollect.size must_== 0
    }

    "remove vertices from the store" in {
      val inMemoryStore = new DefaultStorage
      vertexList.foreach(v => inMemoryStore.vertices.put(v))
      inMemoryStore.vertices.remove(0)
      inMemoryStore.vertices.size must_== vertexList.size - 1
    }
  }

}