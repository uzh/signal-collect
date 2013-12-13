/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

class FastInsertIntSetSpec extends FlatSpec with ShouldMatchers with Checkers {

  implicit lazy val arbInt = Arbitrary(Gen.chooseNum(Int.MinValue, Int.MaxValue))

  "FastInsertIntSet" should "work correctly when empy" in {
    check(
      (item: Int) => {
        val fastInsertSet = Ints.createEmptyFastInsertIntSet
        new FastInsertIntSet(fastInsertSet).contains(item) == false
      },
      minSuccessful(1000))
  }

  it should "support insert into empty set" in {
    check(
      (item: Int) => {
        var wasInserted = false
        try {
          println(s"Inserting $item")
          val fastInsertSet = Ints.createEmptyFastInsertIntSet
          val updatedSet = new FastInsertIntSet(fastInsertSet).insert(item)
          print(updatedSet.mkString(", "))
          wasInserted = new FastInsertIntSet(fastInsertSet).contains(item)
          println(" SUCCESS: " + wasInserted)
        } catch {
          case t: Throwable => t.printStackTrace
        }
        wasInserted
      },
      minSuccessful(1000))
  }

  //  it should "store sets of Ints" in {
  //    check(
  //      (ints: Array[Int]) => {
  //        val intSet = ints.toSet
  //        val compact = Ints.createCompactSet(ints)
  //        new IntSet(compact).toSet == intSet
  //      },
  //      minSuccessful(1000))
  //  }
  //
  //  it should "support the 'contains' operation" in {
  //    check(
  //      (ints: Array[Int], item: Int) => {
  //        val intSet = ints.toSet
  //        val compact = Ints.createCompactSet(ints)
  //        new IntSet(compact).contains(item) == intSet.contains(item)
  //      },
  //      minSuccessful(1000))
  //  }
  //
  //  it should "support the 'insert' operation" in {
  //    check(
  //      (ints: Set[Int], item: Int) => {
  //        try {
  //          val intSet = ints + item
  //          val compact = Ints.createCompactSet(ints.toArray)
  //          val afterInsert = new IntSet(compact).insert(item)
  //          intSet == new IntSet(afterInsert).toSet
  //        } catch {
  //          case t: Throwable =>
  //            t.printStackTrace
  //            throw t
  //        }
  //      },
  //      minSuccessful(10000))
  //  }

}
