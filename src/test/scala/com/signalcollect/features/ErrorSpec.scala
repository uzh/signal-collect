/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 Cotiviti
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

package com.signalcollect.features

import akka.testkit.EventFilter
import com.signalcollect.TestGraph
import org.scalatest.fixture.FlatSpec
import org.scalatest.fixture.UnitFixture
import com.signalcollect.DataGraphVertex
import com.signalcollect.GraphEditor

class TestError() extends Exception("Just a test exception.")

class ErrorSpec extends FlatSpec with UnitFixture {

  "Signal/Collect" should "log errors during vertex initialization" in new TestGraph {
    EventFilter[TestError]() intercept {
      g.addVertex(new DataGraphVertex(1, 1) {
        override def afterInitialization(graphEditor: GraphEditor[Any, Any]) = throw new TestError
        def collect: Int = 1
      })
    }
  }

}
