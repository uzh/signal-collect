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

import com.signalcollect._
import com.signalcollect.configuration._
import com.signalcollect.interfaces._
import collection.JavaConversions._
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.signalcollect.nodeprovisioning.torque.LocalHost
import com.signalcollect.nodeprovisioning.local.LocalNodeProvisioner
import com.signalcollect.nodeprovisioning.Node
import com.signalcollect.nodeprovisioning.local.LocalNode
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge

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
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }

      val graph = GraphBuilder.withMessageBusFactory(new BulkAkkaMessageBusFactory(1000)).build
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }

      graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.PureAsynchronous).withCollectThreshold(0).withSignalThreshold(0.00001))
      var allcorrect = graph.aggregate(new AggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      allcorrect
    }

    "can handle bulk size of 1" in {
      def pageRankFiveCycleVerifier(v: Vertex[_, _]): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val expectedState = 1.0
        val correct = (state - expectedState).abs < 0.0001
        if (!correct) {
          System.out.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }

      val graph = GraphBuilder.withMessageBusFactory(new BulkAkkaMessageBusFactory(1)).build
      for (i <- 0 until 5) {
        val v = new PageRankVertex(i)
        graph.addVertex(v)
        graph.addEdge(i, new PageRankEdge((i + 1) % 5))
      }

      graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.PureAsynchronous).withCollectThreshold(0).withSignalThreshold(0.00001))
      var allcorrect = graph.aggregate(new AggregationOperation[Boolean] {
        val neutralElement = true
        def aggregate(a: Boolean, b: Boolean): Boolean = a && b
        def extract(v: Vertex[_, _]): Boolean = pageRankFiveCycleVerifier(v)
      })
      allcorrect
    }

  }
}