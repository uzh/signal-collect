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

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbContainer
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers
import com.signalcollect.TestAnnouncements

class HasSetSpec extends FlatSpec with Checkers with TestAnnouncements {

  "HasSet" should "support inserts" in {
    check(
      (setEntries: Array[String]) => {
        var wasEqual = false
        val s = new HashSet[String](8, 0.5f)
        try {
          val sCorrect = setEntries.toSet
          for (e <- setEntries) {
            s.add(e)
          }
          wasEqual = s.toScalaSet == sCorrect
          if (!wasEqual) {
            println("Problematic set: " + s.toScalaSet +
              "\nShould have been: " + sCorrect.toString)
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
      (toAdd: Array[String], toAddThenRemove: Array[String]) => {
        val setEntries = toAdd.toSet ++ toAddThenRemove.toSet
        var wasEqual = false
        var sCorrect = setEntries.toSet
        val s = new HashSet[String](8, 0.5f)
        try {
          for (e <- setEntries) {
            s.add(e)
          }
          for (e <- toAddThenRemove) {
            s.remove(e)
            sCorrect -= e
          }
          wasEqual = s.toScalaSet == sCorrect
          if (!wasEqual) {
            println("Problematic set: " + s.toScalaSet +
              "\nShould have been: " + sCorrect.toString)
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
