/*
 *  @author Philip Stutz
 *  
 *  Copyright 2011 University of Zurich
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

import com.signalcollect.api._
import com.signalcollect.configuration._

/**
 *  Regular expression to match links in Html strings
 */
object Regex {
  val hyperlink = """<a\s+href=(?:"([^"]+)"|'([^']+)').*?</a>""".r
}

/**
 *  Proof of concept combination of the PageRank vertices with a WebCrawler
 *  Do not use on a larger scale:
 *  	- does not respect robots.txt
 *  	- lacks proper user agent string
 */
object WebCrawler extends App {
  val cg = new ComputeGraphBuilder().build
  cg.addVertex(new Webpage("http://www.ifi.uzh.ch/ddis/", 2))
  val stats = cg.execute
  cg.foreachVertex(println(_))
  println(stats)
  cg.shutdown
}

/**
 *  Adds linked webpages as vertices to the graph and connects them with a link edge
 */
class Webpage(id: String, crawlDepth: Int, dampingFactor: Double = 0.85) extends Page(id, dampingFactor) {
  override def afterInitialization {
    if (crawlDepth > 0) {
      try {
        val webpage = io.Source.fromURL(id, "ISO-8859-1").mkString
        Regex.hyperlink.findAllIn(webpage).matchData map (_.group(1)) foreach { linked =>
          addVertex(new Webpage(linked, crawlDepth - 1))
          addEdge(new Link(id, linked))
        }
      } catch {
        case _ =>
      }
    }
  }
}