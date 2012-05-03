/*
 *  @author Philip Stutz
 *  
 *  Copyright 2012 University of Zurich
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
 *  
 */

package com.signalcollect.util.collections

import org.junit.Test
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher

import scala.collection.mutable.{ IndexedSeq, ArrayBuffer, ListBuffer }

@RunWith(classOf[JUnitRunner])
class ScoredSetWithAverageSpec extends SpecificationWithJUnit {

  "Scored set with average" should {

    "have the right initial average" in {
      val set = new ScoredSetWithAverage[Any]()
      set.getAverage must_== 0.0
    }

    "correctly add one item" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add(1, 1.0)
      set.getAverage must_== 1.0
    }

    "correctly add two items" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.getAverage must_== 1.0
    }

    "correctly add three items" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.add("c", 4.0)
      set.getAverage must_== 2.0
    }

    "return the correct element" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.add("c", 4.0)
      set.nextAboveAverageItem must_== "c"
      set.nextAboveAverageItem must_== "c"
      set.nextAboveAverageItem must_== "c"
    }

    "return the correct element after an update" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.add("c", 4.0)
      set.nextAboveAverageItem must_== "c"
      set.nextAboveAverageItem must_== "c"
      set.nextAboveAverageItem must_== "c"
      set.updateItemScore("a", 10.0)
      set.nextAboveAverageItem must_== "a"
      set.nextAboveAverageItem must_== "a"
      set.nextAboveAverageItem must_== "a"
    }

    "have the correct average score after an update" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.add("c", 4.0)
      set.updateItemScore("a", 10.0)
      set.getAverage must_== 5.0
    }

    "return the correct element after a removal" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add("a", 1.0)
      set.add("b", 1.0)
      set.add("c", 4.0)
      set.updateItemScore("a", 10.0)
      set.remove("a")
      set.nextAboveAverageItem must_== "c"
    }

    "not compute a wrong average" in {
      val set = new ScoredSetWithAverage[Any]()
      for (i <- 0 to 1000000) {
        set.add(i, 1.0 / 100000000.1)
      }
      set.add(1000001, 1.0 / 100000000)
      set.nextAboveAverageItem must_== 1000001
    }
    
    "not divide by zero when all elements are removed" in {
      val set = new ScoredSetWithAverage[Any]()
      set.add(1, 1.0)
      set.remove(1)
      set.getAverage must_== 0.0
    }

  }
}