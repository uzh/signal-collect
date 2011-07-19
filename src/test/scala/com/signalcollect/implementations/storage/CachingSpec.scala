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

package com.signalcollect.implementations.storage

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import com.signalcollect.examples.Page

@RunWith(classOf[JUnitRunner])
class CachingSpec extends SpecificationWithJUnit with Mockito {
  "LRU caching" should {
    val storage = mock[VertexStore]
  
    val v0 = mock[Vertex[Int, Float]]
    v0.id returns 0
    v0.state returns 0
    v0.toString returns "Mock for Vertex with ID: " + v0.id

    val v1 = mock[Vertex[Int, Float]]
    v1.id returns 1
    v1.state returns 0
    v1.toString returns "Mock for Vertex with ID: " + v1.id

    val v2 = mock[Vertex[Int, Float]]
    v2.id returns 2
    v2.state returns 0
    v2.toString returns "Mock for Vertex with ID: " + v2.id

    val v3 = mock[Vertex[Int, Float]]
    v3.id returns 3
    v3.state returns 0
    v3.toString returns "Mock for Vertex with ID: " + v3.id

    val v4 = mock[Vertex[Int, Float]]
    v4.id returns 4
    v4.state returns 0
    v4.toString returns "Mock for Vertex with ID: " + v4.id

    val vertexList = List(v0, v1, v2, v3, v4)

    "keep only the most recently inserted vertices" in {
      val lruCache = new LRUMap[Any, Vertex[_,_]](storage, 4)
      vertexList.foreach(v => lruCache.put(v.id, v))
      lruCache.get(0) === None
      lruCache.get(1).get.asInstanceOf[Vertex[Int, Float]] === v1
      lruCache.get(2).get.asInstanceOf[Vertex[Int, Float]] === v2
      lruCache.get(3).get.asInstanceOf[Vertex[Int, Float]] === v3
      lruCache.get(4).get.asInstanceOf[Vertex[Int, Float]] === v4
    } 

    "work in a LRU fashion" in {
      val lruCache2 = new LRUMap[Any, Vertex[_,_]](storage, 4)
      lruCache2.put(v0.id, v0)
      lruCache2.put(v1.id, v1)
      lruCache2.put(v2.id, v2)
      lruCache2.put(v3.id, v3)
      lruCache2.get(0) // simulate a usage
      lruCache2.put(v4.id, v4)
      lruCache2.get(0).get.asInstanceOf[Vertex[Int, Float]] === v0
      lruCache2.get(1) === None
      lruCache2.get(2).get.asInstanceOf[Vertex[Int, Float]] === v2
      lruCache2.get(3).get.asInstanceOf[Vertex[Int, Float]] === v3
      lruCache2.get(4).get.asInstanceOf[Vertex[Int, Float]] === v4
    }
    
    "call foreach on all of them" in {
      val lruCache3 = new LRUMap[Any, Vertex[_,_]](storage, 4)
      lruCache3.put(v0.id, v0)
      lruCache3.put(v1.id, v1)
      lruCache3.put(v2.id, v2)
      lruCache3.put(v3.id, v3)
      lruCache3.get(0).get.asInstanceOf[Vertex[Int, Float]] === v0
      lruCache3.get(1).get.asInstanceOf[Vertex[Int, Float]] === v1
      lruCache3.get(2).get.asInstanceOf[Vertex[Int, Float]] === v2
      lruCache3.get(3).get.asInstanceOf[Vertex[Int, Float]] === v3
    }
  }
}

import java.util.Map.Entry

class Cache(capacity: Int) extends java.util.LinkedHashMap[Any, Any](capacity + 1, 1.1f, false) {

  override def removeEldestEntry(eldest: Entry[Any, Any]): Boolean = {
    size() > capacity;
  }
}