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
import com.signalcollect.util.TestAnnouncements

class LoadingSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  "Loader" should "correctly parse vertex data from an ordered integer edge list" in {
    val sep = File.separator
    val testFilePath = s".${sep}test-data${sep}notredame2"
    val d = Loading.intEdgeIdsOrderedBySourceId(testFilePath).toList
    assert(d == List((0, List(1, 0)), (1, List(0)), (3, List(2))))
  }

}
