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

class LongBitSetSpec extends FlatSpec with ShouldMatchers with Checkers {

  "LongBitSet" should "correctly retrieve all entries when all bits are set" in {
    val longBitSet = new LongBitSet(-1l) // -1 has all bits set (2's complement)
    longBitSet.toSet == (0 to 63).toSet
  }

  it should "support base values" in {
    val longBitSet = new LongBitSet(-1l) // -1 has all bits set (2's complement)
    longBitSet.foreachWithBaseValue(println(_), 10)
    longBitSet.toSetWithBaseValue(10) == (10 to 73).toSet
  }
  
}
