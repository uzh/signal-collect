/*
 *  @author Tobias Bachmann
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
 */

package com.signalcollect.deployment

import scala.collection.immutable.HashMap

import org.scalatest.FlatSpec
import org.scalatest.mock.EasyMockSugar
import org.scalatest.prop.Checkers

import com.signalcollect.ExecutionInformation
import com.signalcollect.Graph
import com.signalcollect.GraphBuilder
import com.signalcollect.examples.PageRankEdge
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.interfaces.NodeActor
import com.signalcollect.node.DefaultNodeActor


import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.TestActorRef

class AlgorithmSpec extends FlatSpec with Checkers with EasyMockSugar {

  trait DefaultAlgorithm {
    val defaultAlgorithm = TestAlgorithm
  }

  trait GbMock {
    val gbMock = mock[GraphBuilder[Any, Any]]
  }

  trait GraphMock {
    val graphMock = mock[Graph[Any, Any]]
  }

  trait StatsMock {
    val statsMock = mock[ExecutionInformation[Any, Any]]
  }

  trait ASystem {
    implicit val system = ActorSystem("SignalCollect")
  }

  trait NodeActorArray extends ASystem {
    val nodeActors = Array(TestActorRef(new DefaultNodeActor[Any, Any](
      actorNamePrefix = "prefix",
      nodeId = 0,
      numberOfNodes = 0,
      fixedNumberOfCores = None,
      nodeProvisionerAddress = None)).asInstanceOf[ActorRef])
  }

  "DeployableAlgorithm" should "create a GraphBuilder with NodeActors and Actorsystem" in
    new GbMock with NodeActorArray with DefaultAlgorithm {
      expecting {
        gbMock.withActorSystem(system).andReturn(gbMock)
        gbMock.withPreallocatedNodes(nodeActors).andReturn(gbMock)
      }
      whenExecuting(gbMock) {
        defaultAlgorithm.createDefaultGraphBuilder(Some(nodeActors), Some(system), gbMock)
      }
    }

  it should "create a GraphBuilder with an ActorSystem" in new DefaultAlgorithm with GbMock with ASystem {
    expecting {
      gbMock.withActorSystem(system).andReturn(gbMock)
    }
    whenExecuting(gbMock) {
      defaultAlgorithm.createDefaultGraphBuilder(None, Some(system), gbMock)
    }
  }

  it should "create a GraphBuilder with NodeActors" in
    new DefaultAlgorithm with GbMock with NodeActorArray {
      expecting {
        gbMock.withPreallocatedNodes(nodeActors).andReturn(gbMock)
      }
      whenExecuting(gbMock) {
        defaultAlgorithm.createDefaultGraphBuilder(Some(nodeActors), None, gbMock)
      }
    }

  it should "have a default implementation of configureGraphBuilder, that gives back graphBuilder untouched" in
    new DefaultAlgorithm with GbMock {
      expecting {
        //nothing
      }
      whenExecuting(gbMock) {
        val untouchedGb = defaultAlgorithm.configureGraphBuilder(gbMock)
        assert(untouchedGb === gbMock)
      }
    }

  it should "have a default implementation of execute, that only calls execut on graph" in
    new DefaultAlgorithm with GraphMock with StatsMock {
      expecting {
        graphMock.execute.andReturn(statsMock)
      }
      whenExecuting(graphMock) {
        val result = defaultAlgorithm.execute(graphMock)
        assert(result._1 === statsMock)
        assert(result._2 === graphMock)
      }
    }

  it should "have a default implementation of reportResuls, which prints the stats to the console" in
    new DefaultAlgorithm with GraphMock with StatsMock {
      expecting {
        statsMock.toString
      }
      whenExecuting(statsMock) {
        defaultAlgorithm.reportResults(statsMock, graphMock)
      }
    }
  
  it should "have a default implementation of paramets, which returns an empty Map" in
    new DefaultAlgorithm {
      assert(defaultAlgorithm.parameters === HashMap[String, String]())
    }

  object TestAlgorithm extends Algorithm[Any, Any] {
    override def loadGraph(graph: Graph[Any, Any]): Graph[Any, Any] = {
      graph.addVertex(new PageRankVertex(1))
      graph.addVertex(new PageRankVertex(2))
      graph.addVertex(new PageRankVertex(3))
      graph.addEdge(1, new PageRankEdge(2))
      graph.addEdge(2, new PageRankEdge(1))
      graph.addEdge(2, new PageRankEdge(3))
      graph.addEdge(3, new PageRankEdge(2))
      graph
    }

  }
}