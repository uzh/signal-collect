/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
 *
 *  Copyright 2013 University of Zurich
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
import com.signalcollect.messaging.DefaultVertexToWorkerMapper
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.MapperFactory

class Worker0Mapper[Id] extends VertexToWorkerMapper[Id] {
  def getWorkerIdForVertexId(vertexId: Id): Int = 0
  def getWorkerIdForVertexIdHash(vertexIdHash: Int): Int = 0
}

class Worker0MapperFactory[Id] extends MapperFactory[Id] {
  def createInstance(numberOfNodes: Int, workersPerNode: Int) = new Worker0Mapper
}

/**
 * Unit and integration tests for vertex mappers.
 */
@RunWith(classOf[JUnitRunner])
class MapperSpec extends SpecificationWithJUnit with Serializable {

  sequential

  "Default mapper" should {
    val mapper = new DefaultVertexToWorkerMapper[Int](1, 10)
    "correctly map a vertex to a worker" in {
      mapper.getWorkerIdForVertexId(13) === 3
    }

    "correctly map a vertex hash to a worker" in {
      mapper.getWorkerIdForVertexIdHash(13) === 3
    }
  }

  "Custom mapper" should {
    "correctly support PageRank computation" in {
      def verify(v: Vertex[_, _, _, _], expectedState: Double): Boolean = {
        val state = v.state.asInstanceOf[Double]
        val correct = (state - expectedState).abs < 0.0001
        if (!correct) {
          System.err.println("Problematic vertex:  id=" + v.id + ", expected state=" + expectedState + " actual state=" + state)
        }
        correct
      }
      val graph = GraphBuilder.withMapperFactory(new Worker0MapperFactory[Any]).build
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
          def extract(v: Vertex[_, _, _, _]): Boolean = verify(v, 1.0)
        })
        allcorrect
      } finally {
        graph.shutdown
      }
    }

  }
}