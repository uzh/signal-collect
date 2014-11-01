/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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
 */

package com.signalcollect

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.signalcollect.examples.Location
import com.signalcollect.examples.Path
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.util.TestAnnouncements

class LatencySpec extends FlatSpec with ShouldMatchers with TestAnnouncements {

  "Signal/Collect" should "terminate with a very low latency when an execution does nothing" in {
    val startTime = System.currentTimeMillis
    val g = GraphBuilder.withStatsReportingInterval(10000).build
    try {
      (1 to 50).foreach { i =>
        g.awaitIdle
        val v1 = new Location(1, Some(0))
        val v2 = new Location(2, None)
        g.addVertex(v1)
        g.addVertex(v2)
        g.addEdge(1, new Path(2))
        g.awaitIdle
        //println(g.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous)))
        g.execute
        assert(v2.state == Some(1))
        g.reset
      }
    } finally {
      g.shutdown
    }
    val stopTime = System.currentTimeMillis
    val t = stopTime - startTime
    assert(t < 2000, s"Execution took $t milliseconds, should be less than 2 seconds.")
  }

}
