/*
 *  @author Philip Stutz
 *
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.examples

import com.signalcollect._
import scala.io.Source
import scala.io.Codec
import com.signalcollect.TopKFinder
import sun.net.www.http.HttpClient
import java.net.URL
import java.net.UnknownHostException

class LodNeighbourhoodPageRank(id: String, crawlDepth: Int = 3) extends PageRankVertex[String](id) {

  override def afterInitialization(ge: GraphEditor[Any, Any]) {
    println("Added " + id)
    if (crawlDepth > 0) {
      try {
        val dataUrl = id.replace(
          "http://dbpedia.org/resource/",
          "http://dbpedia.org/data/") +
          ".ntriples"
        val lodSite = Source.fromURL(dataUrl)
        val triples = lodSite.getLines
        val urls = triples.map(_.split("\t")).map(_(2)).flatMap(s =>
          if (s.startsWith("<http://dbpedia.org/resource/")) {
            Some(s.substring(1, s.length - 3))
          } else {
            None
          })
        val neighbours = List[String]()
        for (dbpediaUrl <- urls) {
          ge.addVertex(new LodNeighbourhoodPageRank(dbpediaUrl, crawlDepth - 1))
          ge.addEdge(id, new PageRankEdge(dbpediaUrl))
        }
      } catch {
        case t: Throwable => println("Problem: " + t.toString) // Parse error or not a LOD link, ignore.
      }
    }

  }
}

object LodNeighbourhood extends App {
  val graph = GraphBuilder.build
  graph.addVertex(
    new LodNeighbourhoodPageRank("http://dbpedia.org/resource/Tiger"))
  println("Awaiting idle.")
  graph.awaitIdle
  println("Executing.")
  val stats = graph.execute
  val top20 = graph.aggregate(new TopKFinder[Double](20))
  println(stats)
  println(top20.mkString("\n"))
  graph.shutdown
}
