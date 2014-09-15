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
import com.signalcollect.TestAnnouncements

class BasicBitSetSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  "BasicBitSet" should "correctly retrieve all entries when all bits are set" in {
    val longBitSet = new BasicBitSet(-1l) // -1 has all bits set (2's complement)
    val toSet = longBitSet.toSet
    assert(toSet == (0 to 63).toSet)
    assert(longBitSet.size == 64)
  }

  it should "should be empty before inserts" in {
    val longBitSet = new BasicBitSet(0l)
    val toSet = longBitSet.toSet
    assert(toSet == Set.empty[Int])
    assert(longBitSet.size == 0)
  }

  it should "support an insert into an empty set" in {
    check(
      (item: Int) => {
        val insertItem = (item & Int.MaxValue) % 64
        val longBitSet = new BasicBitSet(new BasicBitSet(0l).set(insertItem))
        assert(longBitSet.toSet === Set(insertItem))
        assert(longBitSet.min === insertItem)
        assert(longBitSet.max === insertItem)
        longBitSet.size === 1
      },
      minSuccessful(100))
  }

  it should "support multiple inserts into an empty set" in {
    check(
      (is: Array[Int]) => {
        val insertItems = is.map(i => (i & Int.MaxValue) % 64)
        var longBitSet = new BasicBitSet(0l)
        var standardSet = Set.empty[Int]
        for (i <- insertItems) {
          longBitSet = new BasicBitSet(longBitSet.set(i))
          standardSet += i
        }
        assert(longBitSet.toSet === standardSet)
        if (is.nonEmpty) {
          assert(longBitSet.min === standardSet.min)
          assert(longBitSet.max === standardSet.max)
        }
        longBitSet.size === standardSet.size
      },
      minSuccessful(100))
  }

  it should "support base values A" in {
    val longBitSet = new BasicBitSet(-1l)
    assert(longBitSet.toSetWithBaseValue(10) === (10 to 73).toSet)
  }

  it should "support base values B" in {
    val longBitSet = new BasicBitSet(1l)
    assert(longBitSet.toSetWithBaseValue(0) === Set(0))
  }

}
