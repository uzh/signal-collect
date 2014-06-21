/*
 *  @author Philip Stutz
 *  @author Tobias Bachmann
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
 *
 */

package com.signalcollect.deployment

import com.signalcollect.util.FileDownloader
import com.signalcollect.GraphBuilder
import java.net.URL
import scala.concurrent._
import java.io.FileInputStream
import java.io.DataInputStream
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.examples.PlaceholderEdge
import com.signalcollect.util.Ints
import java.io.FileReader
import java.io.BufferedReader
import com.signalcollect.GraphEditor
import scala.collection.mutable.ArrayBuffer
import com.signalcollect.Vertex
import com.signalcollect.Edge
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory

object Handlers {
  def nonExistingVertex: (Edge[Any], Any) => Option[Vertex[Any, _]] = {
    (edgedId, vertexId) => Some(new EfficientPageRankVertex(vertexId.asInstanceOf[Int]))
  }
  def undeliverableSignal: (Any, Any, Option[Any], GraphEditor[Any, Any]) => Unit = {
    case (signal, id, sourceId, ge) =>
      ge.addVertex(new EfficientPageRankVertex(id.asInstanceOf[Int]))
      ge.sendSignal(signal, id, sourceId)
  }
}

class TwitterGraphSlow extends DeployableAlgorithm {
  override def execute(parameters: Map[String, String], graphBuilder: GraphBuilder[Any, Any]) {
    println("download graph")
    val url = parameters.get("url").getOrElse("https://s3-eu-west-1.amazonaws.com/signalcollect/user/hadoop/twitterSmall.txt")
    FileDownloader.downloadFile(new URL(url), "twitter_rv.net")
    println("build graph")
    val graph = graphBuilder.
      withMessageSerialization(true).
      //      withThrottlingEnabled(false).
      //      withMessageBusFactory(new BulkAkkaMessageBusFactory(10000, false)).
      //      withAkkaMessageCompression(false).
      //      withHeartbeatInterval(100).
      //      withEagerIdleDetection(true).
      //      withThrottlingEnabled(true).
      //    withConsole(true).
      build
    println("set Handlers")
    graph.setEdgeAddedToNonExistentVertexHandler {
      Handlers.nonExistingVertex
    }
    graph.setUndeliverableSignalHandler {
      Handlers.undeliverableSignal
    }
    graph.awaitIdle
    println("read file and load graph")
    //    val in = new BufferedReader(new FileReader("twitter.txt"))
    val in = new BufferedReader(new FileReader("twitter_rv.net"))
    val beginTime = System.currentTimeMillis()

    var line = in.readLine()
    var cnt = 0

    while (line != null) {
      if (cnt % 1000000 == 0) {
        val elapsedTime = System.currentTimeMillis() - beginTime
        println(s"read $cnt edges in $elapsedTime ms")
      }
      val edge = line.split("\\s").map(_.toInt)
      //      graph.addVertex(new EfficientPageRankVertex(edge(0)))
      //      graph.addVertex(new EfficientPageRankVertex(edge(1)))
      graph.addEdge(edge(1), new PlaceholderEdge(edge(0)))
      line = in.readLine()
      cnt += 1
    }
    graph.awaitIdle
    val end = System.currentTimeMillis() - beginTime
    println(s"file read in $end ms")
    println("execute")
    val stats = graph.execute //(ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive))
    println(stats)

    graph.shutdown

  }
}

