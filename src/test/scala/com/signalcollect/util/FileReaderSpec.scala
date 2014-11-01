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

package com.signalcollect.util

import scala.collection.mutable.ArrayBuffer
import org.scalatest.prop.Checkers
import org.scalatest.ShouldMatchers
import org.scalatest.FlatSpec
import java.io.File

class FileReaderSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  val sep = File.separator
  val testFilePath1 = s".${sep}test-data${sep}ascii-ints.txt"
  val testFilePath2 = s".${sep}test-data${sep}ints-no-newline-at-end.txt"

  "FileReader" should "correctly parse a file with an iterator" in {
    val asList = FileReader.intIterator(testFilePath1).toList
    assert(asList == List(1234, 3525, 123, 436, 43663, 33, 44, 0))
  }

  it should "correctly parse a file with the processing function" in {
    val buffer = new ArrayBuffer[Int]
    FileReader.processInts(testFilePath1, buffer.append(_))
    val asList = buffer.toList
    assert(asList == List(1234, 3525, 123, 436, 43663, 33, 44, 0))
  }

  it should "correctly iterate over ints when there is no newline at the end" in {
    val asList = FileReader.intIterator(testFilePath2).toList
    assert(asList == List(1, 2, 3, 0))
  }

}
