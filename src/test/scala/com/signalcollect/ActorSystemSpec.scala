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

import org.scalatest.{ FlatSpec, Matchers }
import com.signalcollect.examples.{ PageRankEdge, PageRankVertex }
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import com.signalcollect.util.TestAnnouncements

class ActorSystemSpec extends FlatSpec with Matchers with TestAnnouncements {

  "Signal/Collect" should "support multiple instances on the same actor system" in {
    val a = TestConfig.actorSystem(port = 2556)
    val graph1 = GraphBuilder.withActorSystem(a).withActorNamePrefix(TestConfig.prefix).build
    try {
      val graph2 = GraphBuilder.
        withActorSystem(a).
        withActorNamePrefix("b").
        build
      try {
        graph2.addVertex(new PageRankVertex(1))
        graph2.addVertex(new PageRankVertex(2))
        graph2.addVertex(new PageRankVertex(3))
        graph2.addEdge(1, new PageRankEdge(2))
        graph2.addEdge(2, new PageRankEdge(1))
        graph2.addEdge(2, new PageRankEdge(3))
        graph2.addEdge(3, new PageRankEdge(2))
        val s2 = graph2.execute
        graph1.addVertex(new PageRankVertex(1))
        graph1.addVertex(new PageRankVertex(2))
        graph1.addVertex(new PageRankVertex(3))
        graph1.addEdge(1, new PageRankEdge(2))
        graph1.addEdge(2, new PageRankEdge(1))
        graph1.addEdge(2, new PageRankEdge(3))
        graph1.addEdge(3, new PageRankEdge(2))
        val s1 = graph1.execute
        s1.aggregatedWorkerStatistics.numberOfVertices should be(3)
        s2.aggregatedWorkerStatistics.numberOfVertices should be(3)
      } finally {
        graph2.shutdown
      }
    } finally {
      graph1.shutdown
      a.shutdown
    }
  }

  it should "support running on the same actor system with a shutdown in between" in {
    val a = TestConfig.actorSystem(port = 2556)
    val graph1 = GraphBuilder.withActorSystem(a).withActorNamePrefix(TestConfig.prefix).build
    try {
      graph1.addVertex(new PageRankVertex(1))
      graph1.addVertex(new PageRankVertex(2))
      graph1.addVertex(new PageRankVertex(3))
      graph1.addEdge(1, new PageRankEdge(2))
      graph1.addEdge(2, new PageRankEdge(1))
      graph1.addEdge(2, new PageRankEdge(3))
      graph1.addEdge(3, new PageRankEdge(2))
      val s1 = graph1.execute
      s1.aggregatedWorkerStatistics.numberOfVertices should be(3)
    } finally {
      graph1.shutdown
    }
    // Give Akka time to cleanup the old actors with the same names.
    Thread.sleep(1000)
    val graph2 = GraphBuilder.
      withActorSystem(a).
      //withActorNamePrefix("b").
      build
    try {
      graph2.addVertex(new PageRankVertex(1))
      graph2.addVertex(new PageRankVertex(2))
      graph2.addVertex(new PageRankVertex(3))
      graph2.addEdge(1, new PageRankEdge(2))
      graph2.addEdge(2, new PageRankEdge(1))
      graph2.addEdge(2, new PageRankEdge(3))
      graph2.addEdge(3, new PageRankEdge(2))
      val s2 = graph2.execute
      s2.aggregatedWorkerStatistics.numberOfVertices should be(3)
    } finally {
      graph2.shutdown
    }
    a.shutdown
  }

  it should "run on multiple actor systems inside the same JVM" in {
    val a = TestConfig.actorSystem(port = 2556)
    val b = TestConfig.actorSystem(port = 2557)
    val graph1 = GraphBuilder.withActorSystem(a).withActorNamePrefix(TestConfig.prefix).build
    try {
      val graph2 = GraphBuilder.withActorSystem(b).withActorNamePrefix(TestConfig.prefix).build
      try {
        graph2.addVertex(new PageRankVertex(1))
        graph2.addVertex(new PageRankVertex(2))
        graph2.addVertex(new PageRankVertex(3))
        graph2.addEdge(1, new PageRankEdge(2))
        graph2.addEdge(2, new PageRankEdge(1))
        graph2.addEdge(2, new PageRankEdge(3))
        graph2.addEdge(3, new PageRankEdge(2))
        val s2 = graph2.execute
        graph1.addVertex(new PageRankVertex(1))
        graph1.addVertex(new PageRankVertex(2))
        graph1.addVertex(new PageRankVertex(3))
        graph1.addEdge(1, new PageRankEdge(2))
        graph1.addEdge(2, new PageRankEdge(1))
        graph1.addEdge(2, new PageRankEdge(3))
        graph1.addEdge(3, new PageRankEdge(2))
        val s1 = graph1.execute
        s1.aggregatedWorkerStatistics.numberOfVertices should be(3)
        s2.aggregatedWorkerStatistics.numberOfVertices should be(3)
      } finally {
        graph2.shutdown
      }
    } finally {
      graph1.shutdown
      a.shutdown
      b.shutdown
    }
  }

}
