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

package com.signalcollect.features

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GraphBuilder
import com.signalcollect.Vertex
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import org.specs2.runner.JUnitRunner
import com.signalcollect.interfaces.ModularAggregationOperation

/**
 * Hint: For information on how to run specs see the specs v.1 website
 * http://code.google.com/p/specs/wiki/RunningSpecs
 */
@RunWith(classOf[JUnitRunner])
class SnapshotSpec extends SpecificationWithJUnit with Serializable {

  sequential

  "Snapshots" should {
    "correctly store and load a small graph" in {
      def verify(v: Vertex[_, _], expectedState: Double): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val correct = (state - expectedState).abs < 0.0001
        if (!correct) {
          System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      val graph = GraphBuilder.build
      graph.deleteSnapshot
      graph.restore
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }
      graph.snapshot
      graph.execute(ExecutionConfiguration.
        withExecutionMode(ExecutionMode.PureAsynchronous).
        withCollectThreshold(0).
        withSignalThreshold(0.00001))
      graph.restore
      var allcorrect = graph.aggregate(new ModularAggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _]): Boolean = verify(v, 0.15)
      })
      graph.deleteSnapshot
      graph.shutdown
      allcorrect
    }

  }
}