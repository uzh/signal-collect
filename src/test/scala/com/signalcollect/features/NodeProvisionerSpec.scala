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

package com.signalcollect.features

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner

import com.signalcollect._
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner

@RunWith(classOf[JUnitRunner])
class NodeProvisionerSpec extends SpecificationWithJUnit {

  sequential

  "Signal/Collect" should {

    "support setting the number of workers created" in {
      val numberOfWorkers = 100
      val graph = GraphBuilder.withNodeProvisioner(new LocalNodeProvisioner(Some(numberOfWorkers))).build
      try {
        val stats = graph.execute
        stats.individualWorkerStatistics.length == numberOfWorkers
      } finally {
        graph.shutdown
      }
    }
  }
}
