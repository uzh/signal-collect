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

import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.prop.Checkers
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import org.scalacheck.Arbitrary

class IteratorConcatenatorSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  "IteratorConcatenator" should "correctly concatenate multiple iterators" in {
    val c = new IteratorConcatenator[Int]
    for (i <- 1 to 1000 by 10) {
      c.appendIterator((i until i + 10).iterator)
    }
    c.appendIterator(Seq(1001, 1002, 1003).iterator)
    c.appendIterator(Set(1004).iterator)
    assert(c.toList == (1 to 1004).toList)
  }

  it should "should work with random appends" in {
    check(
      (is: List[List[Int]]) => {
        val c = new IteratorConcatenator[Int]
        is.foreach { list => c.appendIterator(list.iterator) }
        assert(c.toList == is.flatMap(_.iterator).toList)
        true
      },
      minSuccessful(100))
  }

}
