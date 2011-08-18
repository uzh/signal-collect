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
import java.util.Map.Entry
import com.signalcollect.Vertex

@RunWith(classOf[JUnitRunner])
class CachingSpec extends SpecificationWithJUnit with Mockito {
  "LRU caching" should {
    val storage = mock[VertexStore]
  
    val v0 = new Page(0, 0.85)
    val v1 = new Page(1, 0.85)
    val v2 = new Page(2, 0.85)
    val v3 = new Page(3, 0.85)
    val v4 = new Page(4, 0.85)
    
    val vertexList = List(v0, v1, v2, v3, v4)
    

    "keep only the most recently inserted vertices" in {
      val lruCache = new LRUMap[Any, Vertex](storage, 4)
      vertexList.foreach(v => lruCache.put(v.id, v))
      lruCache.get(0) === None
      lruCache.get(1).get.asInstanceOf[Vertex] === v1
      lruCache.get(2).get.asInstanceOf[Vertex] === v2
      lruCache.get(3).get.asInstanceOf[Vertex] === v3
      lruCache.get(4).get.asInstanceOf[Vertex] === v4
    } 

    "work in a LRU fashion" in {
      val lruCache2 = new LRUMap[Any, Vertex](storage, 4)
      lruCache2.put(v0.id, v0)
      lruCache2.put(v1.id, v1)
      lruCache2.put(v2.id, v2)
      lruCache2.put(v3.id, v3)
      lruCache2.get(0) // simulate a usage
      lruCache2.put(v4.id, v4)
      lruCache2.get(0).get.asInstanceOf[Vertex] === v0
      lruCache2.get(1) === None
      lruCache2.get(2).get.asInstanceOf[Vertex] === v2
      lruCache2.get(3).get.asInstanceOf[Vertex] === v3
      lruCache2.get(4).get.asInstanceOf[Vertex] === v4
    }
    
    "call foreach on all of them" in {
      val lruCache3 = new LRUMap[Any, Vertex](storage, 4)
      lruCache3.put(v0.id, v0)
      lruCache3.put(v1.id, v1)
      lruCache3.put(v2.id, v2)
      lruCache3.put(v3.id, v3)
      lruCache3.get(0).get.asInstanceOf[Vertex] === v0
      lruCache3.get(1).get.asInstanceOf[Vertex] === v1
      lruCache3.get(2).get.asInstanceOf[Vertex] === v2
      lruCache3.get(3).get.asInstanceOf[Vertex] === v3
    }
      
  }
}