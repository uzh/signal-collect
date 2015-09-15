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

import org.scalatest.{ Finders, FlatSpec, Matchers }
import com.signalcollect.{TestConfig, ExecutionConfiguration, GraphBuilder, Vertex}
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.examples.{ PageRankEdge, PageRankVertex }
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import com.signalcollect.interfaces.ModularAggregationOperation
import com.signalcollect.util.TestAnnouncements

class BulkSignalingSpec extends FlatSpec with Matchers with TestAnnouncements {

  "Bulk signaling" should "deliver correct results on a 5-cycle graph" in {
    def pageRankFiveCycleVerifier(v: Vertex[_, _, _, _]): Boolean = {
      val state = v.state.asInstanceOf[Double]
      val expectedState = 1.0
      val correct = (state - expectedState).abs < 0.0001
      if (!correct) {
        System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
      }
      correct
    }

    val system = TestConfig.actorSystem(port = 2556)
    val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(TestConfig.prefix).
      withMessageBusFactory(new BulkAkkaMessageBusFactory(1000, false)).build
    try {
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
        def extract(v: Vertex[_, _, _, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      allcorrect
    } finally {
      graph.shutdown
      system.shutdown()
    }
  }

  it should "handle a bulk size of 1 correctly" in {
    def pageRankFiveCycleVerifier(v: Vertex[_, _, _, _]): Boolean = {
      val state = v.state.asInstanceOf[Double]
      val expectedState = 1.0
      val correct = (state - expectedState).abs < 0.0001
      if (!correct) {
        System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
      }
      correct
    }

    val system = TestConfig.actorSystem(port = 2556)
    val graph = GraphBuilder.withActorSystem(system).withActorNamePrefix(TestConfig.prefix)
      .withMessageBusFactory(new BulkAkkaMessageBusFactory(1, true)).build
    try {
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }

      graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.PureAsynchronous).withCollectThreshold(0).withSignalThreshold(0.00001))
      var allcorrect = graph.aggregate(new ModularAggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _, _, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      allcorrect
    } finally {
      graph.shutdown
      system.shutdown()
    }
  }

}
