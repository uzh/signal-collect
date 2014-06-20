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

/** Builds a PageRank compute graph and executes the computation */
class TwitterGraphSlow extends DeployableAlgorithm {
  override def execute(parameters: Map[String, String], graphBuilder: GraphBuilder[Any, Any]) {
//    println("download graph")
//    FileDownloader.downloadFile(new URL("https://s3-eu-west-1.amazonaws.com/signalcollect/user/hadoop/twitter_rv.net.gz"), "twitter_rv.net.gz")
//    println("decompress graph")
//    FileDownloader.decompressGzip("twitter_rv.net.gz", "twitter_rv.net")
//    Thread.sleep(1000)
//    val in = new BufferedReader(new FileReader("twitter_rv.net"))
    println("download graph")
    FileDownloader.downloadFile(new URL("https://s3-eu-west-1.amazonaws.com/signalcollect/user/hadoop/twitterSmall.txt.gz"), "twitter.txt.gz")
    println("decompress graph")
    FileDownloader.decompressGzip("twitter.txt.gz", "twitter.txt")
    Thread.sleep(1000)
    val in = new BufferedReader(new FileReader("twitter.txt"))
    val graph = graphBuilder.
     withEagerIdleDetection(false).
      withThrottlingEnabled(true).
      //    withConsole(true).
      build
    graph.awaitIdle
    println("read file and build graph")
    val beginTime = System.currentTimeMillis()

        var line = in.readLine()
        while (line != null) {
          val edge = line.split("\\s").map(_.toInt)
          graph.addVertex(new EfficientPageRankVertex(edge(0)))
          graph.addVertex(new EfficientPageRankVertex(edge(1)))
          graph.addEdge(edge(1), new PlaceholderEdge(edge(0)))
          line = in.readLine()
        }
    println("loading graph")
    graph.awaitIdle
    val end = System.currentTimeMillis() - beginTime
    println(s"file read in $end ms")
    println("execute")
    val stats = graph.execute //(ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive))
    println(stats)

    graph.shutdown

  }
}

