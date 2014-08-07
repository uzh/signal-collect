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

class BitSetSpec extends FlatSpec with ShouldMatchers with Checkers {

  implicit lazy val arbInt = Arbitrary(Gen.chooseNum(Int.MinValue, Int.MaxValue))

  "BitSet" should "store all ints up to 63" in {
    val bitIntSet = new BitSet(BitSet.create(0, 64))
    for (i <- 0 to 63) {
      val inserted = bitIntSet.insert(i)
      assert(inserted)
    }
    val toSet = bitIntSet.toSet
    assert(toSet === (0 to 63).toSet)
    assert(toSet.size === 64)
  }

  it should "support an insert into an empty set" in {
    check(
      (item: Int) => {
        val insertItem = (item & Int.MaxValue) % 200
        val bitSet = new BitSet(BitSet.create(0, 200))
        bitSet.insert(insertItem)
        println(s"Inserted $insertItem")
        bitSet.foreach(println(_))
        val asSet = bitSet.toSet
        println(s"insertItem = $insertItem, asSet = $asSet")
        assert(asSet === Set(insertItem))
        assert(bitSet.min === insertItem)
        assert(bitSet.max === insertItem)
        bitSet.size === 1
      },
      minSuccessful(100))
  }

  it should "store some Ints" in {
    val bitIntSet = new BitSet(BitSet.create(5, 200))
    var inserted = bitIntSet.insert(6)
    assert(inserted == true)
    inserted = bitIntSet.insert(10)
    assert(inserted == true)
    inserted = bitIntSet.insert(10)
    assert(inserted == false)
    inserted = bitIntSet.insert(13)
    assert(inserted == true)
    inserted = bitIntSet.insert(68)
    assert(inserted == true)
    inserted = bitIntSet.insert(132)
    assert(inserted == true)
    assert(bitIntSet.toSet === Set(6, 10, 13, 68, 132))
  }

  it should "store sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        val maxSize = 10000000
        val mappedInts = ints.map(i => ((i & Int.MaxValue) % maxSize) + 10)
        val intSet = mappedInts.toSet
        val smallest = mappedInts.min
        val bitIntSet = new BitSet(BitSet.create(smallest, maxSize + smallest))
        for (i <- mappedInts) {
          val inserted = bitIntSet.insert(i)
          assert(inserted == true)
        }
        if (intSet != bitIntSet.toSet) {
          println(s"Is: ${bitIntSet.toSet} should: ${intSet}")
        }
        intSet === bitIntSet.toSet
      },
      minSuccessful(1000))
  }

  //  it should "support the 'contains' operation" in {
  //    check(
  //      (ints: Array[Int], item: Int) => {
  //        val intSet = ints.toSet
  //        val compact = Ints.createCompactSet(ints)
  //        new IntSet(compact).contains(item) == intSet.contains(item)
  //      },
  //      minSuccessful(1000))
  //  }

}
