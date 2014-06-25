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

import java.io.BufferedReader
import java.io.FileReader

import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.examples.PlaceholderEdge


case class FileLoader(fileName: String) extends Iterator[GraphEditor[Int, Double] => Unit] {
  lazy val in = new BufferedReader(new FileReader(fileName))
  var cnt = 0
  def addEdge(vertices: (Vertex[Int, _], Vertex[Int, _]))(graphEditor: GraphEditor[Int, Double]) {
    //    graphEditor.addVertex(vertices._1)
    //    graphEditor.addVertex(vertices._2)
    graphEditor.addEdge(vertices._2.id, new PlaceholderEdge[Int](vertices._1.id).asInstanceOf[Edge[Int]])
  }

  def hasNext = {
    cnt < 999999
  }

  def next: GraphEditor[Int, Double] => Unit = {
    cnt += 1
    val line = in.readLine()
    val edge = line.split("\\s").map(_.toInt)
    val target = edge(0)
    val source = edge(1)
    val vertices = (new EfficientPageRankVertex(edge(0)), new EfficientPageRankVertex(edge(1)))

    addEdge(vertices) _
  }
}
