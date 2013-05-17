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
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import com.signalcollect.factory.worker.DistributedWorker
import org.specs2.runner.JUnitRunner
import com.signalcollect.interfaces.ModularAggregationOperation

/**
 * Hint: For information on how to run specs see the specs v.1 website
 * http://code.google.com/p/specs/wiki/RunningSpecs
 */
@RunWith(classOf[JUnitRunner])
class BulkSignalingSpec extends SpecificationWithJUnit with Serializable {

  sequential

  "Bulk signaling" should {
    "deliver correct results on a 5-cycle graph" in {
      def pageRankFiveCycleVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.0001
        if (!correct) {
          System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }

      val graph = GraphBuilder.withWorkerFactory(DistributedWorker).
        withMessageBusFactory(new BulkAkkaMessageBusFactory(1000, false)).build
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }

      graph.execute(ExecutionConfiguration.
        withExecutionMode(ExecutionMode.PureAsynchronous).
        withCollectThreshold(0).
        withSignalThreshold(0.00001))
      var allcorrect = graph.aggregate(new ModularAggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      graph.shutdown
      allcorrect
    }

    "handle a bulk size of 1 correctly" in {
      def pageRankFiveCycleVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.0001
        if (!correct) {
          System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }

      val graph = GraphBuilder.withMessageBusFactory(new BulkAkkaMessageBusFactory(1, true)).build
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }

      graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.PureAsynchronous).withCollectThreshold(0).withSignalThreshold(0.00001))
      var allcorrect = graph.aggregate(new ModularAggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      graph.shutdown
      allcorrect
    }

  }
}