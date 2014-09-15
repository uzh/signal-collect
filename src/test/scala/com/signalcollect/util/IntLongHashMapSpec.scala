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
import com.signalcollect.TestAnnouncements

class IntLongHashMapSpec extends FlatSpec with Checkers with TestAnnouncements {

  "IntLongHashMap" should "support inserts" in {
    check(
      (keyValuePairs: Array[(Int, Long)]) => {
        val validPairs = keyValuePairs.filter(_._1 != 0)
        var wasEqual = false
        val m = new IntLongHashMap(8, 0.5f)
        try {
          val mCorrect = validPairs.toMap
          for (p <- validPairs) {
            m.put(p._1, p._2)
          }
          wasEqual = m.toScalaMap == mCorrect
          if (!wasEqual) {
            println("Problematic map: " + m.toScalaMap +
              "\nShould have been: " + mCorrect.toString)
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        wasEqual
      },
      minSuccessful(1000))
  }

  it should "support removals" in {
    check(
      (keyValuePairs1: Array[(Int, Long)], keyValuePairs2: Array[(Int, Long)]) => {
        val keys1 = keyValuePairs1.map(_._1)
        val keyValuePairs = keyValuePairs1.toMap ++ keyValuePairs2.toMap
        val validPairs = keyValuePairs.filter(_._1 != 0)
        var wasEqual = false
        var mCorrect = validPairs.toMap
        val m = new IntLongHashMap(8, 0.5f)
        try {
          for (p <- validPairs) {
            m.put(p._1, p._2)
          }
          for (key <- keys1) {
            m.remove(key)
            mCorrect -= key
          }
          wasEqual = m.toScalaMap == mCorrect
          if (!wasEqual) {
            println("Problematic map: " + m.toScalaMap +
              "\nShould have been: " + mCorrect.toString)
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace
        }
        wasEqual
      },
      minSuccessful(1000))
  }

}
