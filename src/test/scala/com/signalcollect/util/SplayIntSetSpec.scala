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
import scala.util.Random
import com.signalcollect.TestAnnouncements

case class SimpleSplayIntSet(
  val overheadFraction: Float,
  val maxNodeIntSetSize: Int) extends SplayIntSet

class SplayIntSetSpec extends FlatSpec with ShouldMatchers with Checkers with TestAnnouncements {

  implicit lazy val arbInt = Arbitrary(Gen.chooseNum(Int.MinValue, Int.MaxValue))

  "SplayIntSet" should "handle duplicate inserts correctly" in {
    try {
      var splaySet = new SimpleSplayIntSet(0.05f, 3)
      val insert = List(4, 2, 1, 4, 3, 1, 3)
      for (i <- insert) {
        splaySet.insert(i)
        assert(splaySet.contains(i))
      }
      assert(splaySet.size == 4)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }

  it should "support 100000 inserts" in {
    try {
      val start = System.currentTimeMillis
      val factor = 1.0f
      var splaySet = new SimpleSplayIntSet(0.05f, 10000)
      val randomInts = (0 to 100000).map(x => Random.nextInt)
      for (i <- randomInts) {
        splaySet.insert(i)
      }
      val finish = System.currentTimeMillis
      val time = finish - start
      //println("It took " + (time.toDouble / 1000) + " seconds with factor " + factor + ".")
      assert(splaySet.toSet == randomInts.toSet)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }

  it should "support 1 million inserts with split size 10 and 5% overhead" in {
    try {
      val start = System.currentTimeMillis
      var splaySet = new SimpleSplayIntSet(0.05f, 10)
      var standardSet = Set.empty[Int]
      var i = 0
      while (i < 1000000) {
        val insertValue = Random.nextInt.abs
        val splaySizeBefore = splaySet.size
        splaySet.insert(insertValue)
        standardSet += insertValue
        if (splaySet.size != standardSet.size) {
          println(s"Problematic insert: $insertValue")
          println(s"Splay size ${splaySet.size}")
          println(s"Splay size before $splaySizeBefore")
          println(s"Standard size ${standardSet.size}")
          assert(splaySet.size == standardSet.size)
        }
        i += 1
      }
      assert(splaySet.size == standardSet.size)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }

  it should "store sets of Ints with various overheads and split factors" in {
    check(
      (ints: Array[Int], splitSize: Int, overhead: Float) => {
        var wasEqual = true
        var compact = new SplayIntSet {
          def overheadFraction = math.min(math.max(overhead, 0.01f), 1.0f)
          def maxNodeIntSetSize = math.max(splitSize, 3)
        }
        try {
          val intSet = ints.toSet
          for (i <- ints) {
            compact.insert(i)
          }
          wasEqual = compact.toSet == intSet
          if (!wasEqual) {
            println("Problematic set: " + compact.toList.toString +
              "\nShould have been: " + ints.toList.toString)
            println("Done")
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        wasEqual
      },
      minSuccessful(10000))
  }

  it should "support 1 million inserts with split size 100 and 1% overhead" in {
    try {
      val start = System.currentTimeMillis
      var splaySet = new SimpleSplayIntSet(0.01f, 100)
      var standardSet = Set.empty[Int]
      var i = 0
      while (i < 1000000) {
        val insertValue = Random.nextInt.abs % 2000000
        val splaySizeBefore = splaySet.size
        splaySet.insert(insertValue)
        standardSet += insertValue
        if (splaySet.size != standardSet.size) {
          println(s"Problematic insert: $insertValue")
          println(s"Splay size ${splaySet.size}")
          println(s"Splay size before $splaySizeBefore")
          println(s"Standard size ${standardSet.size}")
          assert(splaySet.size == standardSet.size)
          assert(splaySet.contains(i) == standardSet.contains(i))
        }
        i += 1
      }
      assert(splaySet.size == standardSet.size)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }

  it should "store sets of Ints" in {
    check(
      (ints: Array[Int]) => {
        var wasEqual = true
        var splaySet = new SimpleSplayIntSet(0.01f, 3)
        val mappedInts = ints.map(x => (x & Int.MaxValue) % 50)
        try {
          var intSet = Set.empty[Int]
          for (i <- mappedInts) {
            splaySet.insert(i)
            intSet += i
            if (splaySet.contains(i) != intSet.contains(i)) {
              println(s"problems with $i on ${intSet}: was not inserted into splay tree")
            }
          }
          wasEqual = splaySet.toSet == intSet
          if (!wasEqual) {
            println("Problematic set: " + splaySet.toList.toString +
              "\nShould have been: " + intSet.toString)
            println("Done")
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        wasEqual
      },
      minSuccessful(10000))
  }

}
