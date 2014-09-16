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

package com.signalcollect.loading

import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.util.FileReader

/**
 * Loads a graph from an adjacency list format.
 * The data format is: vertexId #outEdges outId1 outId1
 *
 * @param filePath The path of the file that should be loaded.
 * @param combinedVertexBuilder The function that creates a vertex from an ID and an adjacency array.
 * @example An example vertex descritpion for a vertex with ID 1 and two out edges
 *          to vertices with IDs 5 and 6:
 *          1 2 5 6
 * @example The loader is used like this: graphEditor.loadGraph(AdjacencyListLoader[Double]("./testfile", myVertexBuilder))
 * @note All numbers have to be in ASCII/UTF-8 format.
 * @note Each vertex is on its own line.
 * @note The vertex ids have to be positive Ints.
 */
case class AdjacencyListLoader[SignalType](
  filePath: String, combinedVertexBuilder: (Int, Array[Int]) => Vertex[Int, _, Int, SignalType])
  extends Iterator[GraphEditor[Int, SignalType] => Unit] {

  var intIterator: Iterator[Int] = _

  var isInitialized = false

  protected def readNextVertex: Vertex[Int, _, Int, SignalType] = {
    if (intIterator.hasNext) {
      val id = intIterator.next
      val numberOfLinks = intIterator.next
      val outlinks = new Array[Int](numberOfLinks)
      var i = 0
      while (i < numberOfLinks) {
        outlinks(i) = intIterator.next
        i += 1
      }
      combinedVertexBuilder(id, outlinks)
    } else {
      null.asInstanceOf[Vertex[Int, _, Int, SignalType]]
    }
  }

  var nextVertex: Vertex[Int, _, Int, SignalType] = null

  def initialize {
    intIterator = FileReader.intIterator(filePath)
    isInitialized = true
    nextVertex = readNextVertex
  }

  def hasNext = {
    if (!isInitialized) {
      initialize
    }
    nextVertex != null
  }

  def next: GraphEditor[Int, SignalType] => Unit = {
    if (!isInitialized) {
      initialize
    }
    if (nextVertex == null) {
      throw new Exception("next was called when hasNext is false.")
    }
    val v = nextVertex // This is actually important, so the closure doesn't capture the mutable var.
    val loader: GraphEditor[Int, SignalType] => Unit = { ge =>
      ge.addVertex(v)
    }
    nextVertex = readNextVertex
    loader
  }

}
