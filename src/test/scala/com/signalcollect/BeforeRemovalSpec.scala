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

package com.signalcollect

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BeforeRemovalSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "Framework" should {

    "call the beforeRemoval function of a vertex before removing it" in {
      val graph = GraphBuilder.build
      graph.addVertex(new BeforeRemovalVertex)
      graph.removeVertex(1)
      graph.execute
      graph.shutdown
      RemovalDetector.beforeRemovalWorked must_== true
    }

  }

}

object RemovalDetector {
  var beforeRemovalWorked = false
}

class BeforeRemovalVertex extends DataGraphVertex(1, 0) {
  def collect = 0
  override def beforeRemoval(ge: GraphEditor[Any, Any]) = RemovalDetector.beforeRemovalWorked = true
}