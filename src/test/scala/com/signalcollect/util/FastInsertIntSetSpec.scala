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

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbContainer
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

class FastInsertIntSetSpec extends FlatSpec with Checkers with TestAnnouncements {

  implicit lazy val arbInt = Arbitrary(Gen.chooseNum(Int.MinValue, Int.MaxValue))

  "FastInsertIntSet" should "support three inserts" in {
    try {
      val start = System.currentTimeMillis
      val factor = 0.0f
      var fastInsertSet = Ints.createEmptyFastInsertIntSet
      val randomInts = (0 to 100000) //.map(x => Random.nextInt())
      for (i <- randomInts) {
        fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(i, factor)
      }
      val finish = System.currentTimeMillis
      val time = finish - start
      println("It took " + (time.toDouble / 1000) + " seconds with factor " + factor)
      assert(new FastInsertIntSet(fastInsertSet).toSet == randomInts.toSet)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }

  //  "FastInsertIntSet" should "support three inserts" in {
  //    try {
  //      val start = System.currentTimeMillis
  //      val factor = 0.0f
  //      var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //      val randomInts = (-100 to 100000)//.map(x => Random.nextInt())
  //      for (i <- randomInts) {
  //        fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(i, factor)
  //      }
  //      val finish = System.currentTimeMillis
  //      val time = finish - start
  //      println("It took " + (time.toDouble / 1000) + " seconds with factor " + factor)
  //      assert(new FastInsertIntSet(fastInsertSet).toSet == randomInts.toSet)
  //    } catch {
  //      case t: Throwable => t.printStackTrace
  //    }
  //  }

  //  "FastInsertIntSet" should "support three inserts" in {
  //    try {
  //      val start = System.currentTimeMillis
  //      val factor = 0.0f
  //      var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //      val randomInts = (-100 to 100000).map(x => Random.nextInt())
  //      for (i <- randomInts) {
  //        fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(i, factor)
  //      }
  //      val finish = System.currentTimeMillis
  //      val time = finish - start
  //      println("It took " + (time.toDouble / 1000) + " seconds with factor " + factor)
  //      assert(new FastInsertIntSet(fastInsertSet).toSet == randomInts.toSet)
  //    } catch {
  //      case t: Throwable => t.printStackTrace
  //    }
  //  }

  //  "FastInsertIntSet" should "support three inserts" in {
  //    try {
  //      //-1, 0, -3
  //      var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-1)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(0)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-3)
  //      println(new FastInsertIntSet(fastInsertSet).toString)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-3), "-3 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //    } catch {
  //      case t: Throwable => t.printStackTrace
  //    }
  //  }

  //  "FastInsertIntSet" should "support three inserts" in {
  //    try {
  //      //-1, 0, -3
  //      var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-1)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(0)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-3)
  //      println(new FastInsertIntSet(fastInsertSet).toString)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-3), "-3 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //    } catch {
  //      case t: Throwable => t.printStackTrace
  //    }
  //  }

  //  "FastInsertIntSet" should "support three inserts" in {
  //    try {
  //      //0, 268435457, -1
  //      var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(0)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-1)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(-3)
  //      println(new FastInsertIntSet(fastInsertSet).toString)
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-1), "-1 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(-3), "-3 contained")
  //      assert(new FastInsertIntSet(fastInsertSet).contains(0), "0 contained")
  //    } catch {
  //      case t: Throwable => t.printStackTrace
  //    }
  //  }

  //   "FastInsertIntSetR" should "work correctly when empy" in {
  //    check(
  //      (item: Int) => {
  //        val fastInsertSet = Ints.createEmptyFastInsertIntSet
  //        new FastInsertIntSet(fastInsertSet).contains(item) == false
  //      },
  //      minSuccessful(1000))
  //  }
  //
  //  it should "support insert into empty set" in {
  //    check(
  //      (item: Int) => {
  //        var wasInserted = false
  //        try {
  //          println(s"Inserting $item")
  //          val fastInsertSet = Ints.createEmptyFastInsertIntSet
  //          val updatedSet = new FastInsertIntSet(fastInsertSet).insert(item)
  //          println(new FastInsertIntSet(fastInsertSet).toList)
  //          print(updatedSet.mkString(", "))
  //          wasInserted = new FastInsertIntSet(updatedSet).contains(item)
  //          println(" SUCCESS: " + wasInserted)
  //        } catch {
  //          case t: Throwable => t.printStackTrace
  //        }
  //        wasInserted
  //      },
  //      minSuccessful(1000))
  //  }
  //
  //  it should "support two inserts" in {
  //    check(
  //      (item: Int, itemAlreadyInSet: Int) => {
  //        var wasInserted = false
  //        try {
  //          val fastInsertSet = Ints.createEmptyFastInsertIntSet
  //          val nonEmptySet = new FastInsertIntSet(fastInsertSet).insert(itemAlreadyInSet)
  //          println(s"Inserting $item")
  //          val fullSet = new FastInsertIntSet(nonEmptySet).insert(item)
  //          wasInserted = new FastInsertIntSet(fullSet).contains(item)
  //          println(" SUCCESS: " + wasInserted)
  //        } catch {
  //          case t: Throwable => t.printStackTrace
  //        }
  //        wasInserted
  //      },
  //      minSuccessful(1000))
  //  }
  //

  //  "And1" should "support three inserts" in {
  //    check(
  //      (item: Int, itemAlreadyInSet1: Int, itemAlreadyInSet2: Int) => {
  //        var wasInserted = false
  //        try {
  //          var fastInsertSet = Ints.createEmptyFastInsertIntSet
  //          fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(itemAlreadyInSet1)
  //          fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(itemAlreadyInSet2)
  //          fastInsertSet = new FastInsertIntSet(fastInsertSet).insert(item)
  //          wasInserted = new FastInsertIntSet(fastInsertSet).contains(item)
  //          println(" SUCCESS: " + wasInserted)
  //        } catch {
  //          case t: Throwable => t.printStackTrace
  //        }
  //        wasInserted
  //      },
  //      minSuccessful(1000))
  //  }
  //
  "The FastInsertSet" should "store sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        var wasEqual = false
        var compact = Ints.createEmptyFastInsertIntSet
        try {
          val intSet = ints.toSet
          for (i <- ints) {
            compact = new FastInsertIntSet(compact).insert(i, 0.0f)
          }
          wasEqual = new FastInsertIntSet(compact).toSet == intSet
          if (!wasEqual) {
            println("Problematic set: " + new FastInsertIntSet(compact).toString +
              "\nShould have been: " + ints.toList.toString)
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        wasEqual
      },
      minSuccessful(1000))
  }

  it should "be able to determine the smallest element" in {
    check(
      (ints: Array[Int]) => {
        var found = true
        var compact = Ints.createEmptyFastInsertIntSet
        try {
          val intSet = ints.toSet
          if (!intSet.isEmpty) {
            for (i <- ints) {
              compact = new FastInsertIntSet(compact).insert(i)
            }
            found = new FastInsertIntSet(compact).min == intSet.min
            if (!found) {
              println("Problematic set: " + new FastInsertIntSet(compact).toString +
                "\nShould have been: " + ints.toList.toString)
            }
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        found
      },
      minSuccessful(1000))
  }

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
