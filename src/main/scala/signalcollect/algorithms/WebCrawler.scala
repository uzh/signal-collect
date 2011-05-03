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

package signalcollect.algorithms

import signalcollect.api._

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
  val cg = new AsynchronousComputeGraph()
  cg.addVertex(classOf[Webpage], "http://www.ifi.uzh.ch/ddis/", 2, 0.85)
  val stats = cg.execute
  cg.foreach(println(_))
  println(stats)
  cg.shutDown
}

/**
 *  Adds linked webpages as vertices to the graph and connects them with a link edge
 */
class Webpage(id: String, crawlDepth: Int, dampingFactor: Double) extends Page(id, dampingFactor) {
  override def afterInitialization {
    if (crawlDepth > 0) {
      try {
        val webpage = io.Source.fromURL(id, "ISO-8859-1").mkString
        Regex.hyperlink.findAllIn(webpage).matchData map (_.group(1)) foreach { linked =>
          addVertex(classOf[Webpage], linked, crawlDepth - 1, dampingFactor)
          addEdge(classOf[Link], id, linked)
        }
      } catch {
        case _ =>
      }
    }
  }
}