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
import com.signalcollect.examples.PageRankVertex$

@RunWith(classOf[JUnitRunner])
class VertexMapSpec extends SpecificationWithJUnit with Mockito {

  "VertexMap" should {
    val vm = new VertexMap()

    "support puts" in {
      vm.put(new PageRankVertex(1))
      vm.put(new PageRankVertex(2))
      vm.put(new PageRankVertex(3))
      vm.isEmpty must_== false
      vm.size must_== 3
    }

  }

}