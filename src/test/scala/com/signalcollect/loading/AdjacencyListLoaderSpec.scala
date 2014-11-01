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

import scala.collection.mutable.ArrayBuffer
import org.scalatest.prop.Checkers
import org.scalatest.ShouldMatchers
import org.scalatest.FlatSpec
import java.io.File
import com.signalcollect.util.FileReader
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.util.AsciiIntIterator
import com.signalcollect.util.AsciiIntIterator
import com.signalcollect.util.TestAnnouncements

class AdjacencyListLoaderSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  val sep = File.separator
  val testFilePath = s".${sep}test-data${sep}adjacency-list-format"

  "AsciiIntIterator" should "correctly parse the integers from an adjacency list file" in {
    val l = FileReader.intIterator(testFilePath).toList
    val correct = List(1, 0, 4, 1, 5, 2, 3, 1, 5, 4, 5, 0)
    assert(l == correct,
      s"Read Ints were $l instead of the correct $correct.")
  }

  "AdjacencyListLoader" should "correctly parse vertex data from a file" in {
    var count = 0
    var parsed = List.empty[(Int, List[Int])]
    AdjacencyListLoader[Double](testFilePath, {
      case (id, array) =>
        val dummy = new EfficientPageRankVertex(1)
        parsed = (id, array.toList) :: parsed
        dummy
    }).foreach(x => count += 1)
    val correct = List(
      (5, List.empty[Int]),
      (2, List(1, 5, 4)),
      (4, List(5)),
      (1, List.empty[Int]))
    assert(parsed == correct,
      s"Parsed was $parsed instead of the correct $correct.")
  }

}
