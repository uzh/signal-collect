/*
 *  @author Philip Stutz
 *  
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.util.collections

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import com.signalcollect.messaging.DefaultMessageBus
import com.signalcollect.examples.PageRankVertex
import java.io.File
import com.signalcollect.examples.PageRankVertex$
import com.signalcollect.util.collections.IntHashMap

@RunWith(classOf[JUnitRunner])
class IntHashMapSpec extends SpecificationWithJUnit with Mockito {

  "VertexMap" should {

    "support puts" in {
      val vm = new IntHashMap[String](8, 0.99f)
      vm.put(-1, "value-1")
      vm.put(0, "value0")
      vm.put(1, "value1")
      vm.isEmpty must_== false
      vm.size must_== 3
    }

    // There was a bug, where the map was not optimized after processing.
    "optimize after processing one item" in {
      val vm = new IntHashMap[String](8, 0.99f)
      vm.put(0, "value0")
      vm.put(1, "value1")
      vm.put(2, "value2")
      vm.put(3, "value3")
      vm.put(4, "value4")
      vm.put(5, "value5")
      vm.put(6, "value6")
      vm.put(16, "value16")
      vm.process(v => {}, Some(1))
      vm.get(16) != null
    }

    "optimize after processing several items" in {
      val vm = new IntHashMap[String](8, 0.99f)
      vm.put(0, "value0")
      vm.put(1, "value1")
      vm.put(2, "value2")
      vm.put(3, "value3")
      vm.put(4, "value4")
      vm.put(5, "value5")
      vm.put(6, "value6")
      vm.put(16, "value16")
      vm.process(v => {}, Some(4))
      vm.process(v => {}, Some(3))
      vm.get(16) != null
    }

    "remove all elements via processing" in {
      val vm = new IntHashMap[String](8, 0.99f)
      vm.put(0, "value0")
      vm.put(1, "value1")
      vm.put(2, "value2")
      vm.put(3, "value3")
      vm.put(4, "value4")
      vm.put(5, "value5")
      vm.put(6, "value6")
      vm.put(16, "value16")
      vm.process(v => {}, Some(5))
      vm.process(v => {}, Some(3))
      vm.size == 0
    }

// Fix by expecting AssertionError.
//    "handle special keys" in {
//      val vm = new IntHashMap[String](8, 0.99f)
//      vm.put(0, "value0")
//      vm.put(Int.MinValue, "valueInt.MinValue")
//      vm.put(Int.MaxValue, "valueInt.MaxValue")
//      vm.size must_== 3
//      vm.get(0) must_== "value0"
//      vm.get(Int.MinValue) must_== "valueInt.MinValue"
//      vm.get(Int.MaxValue) must_== "valueInt.MaxValue"
//    }
  }

}