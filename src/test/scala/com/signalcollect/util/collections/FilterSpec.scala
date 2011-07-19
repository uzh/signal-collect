/*
 *  @author Lorenz Fischer
 *  
 *  Copyright 2011 University of Zurich
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
class FilterSpec extends SpecificationWithJUnit {

  "Filter.bySuperClass" should {
    "not filter when invoked with ListBuffer and ListBuffer" in {
      Filter.bySuperClass(classOf[ListBuffer[Any]], new ListBuffer[Any]) must beSome
    }

    "not filter when invoked with IndexedSeq and ArrayBuffer" in {
      Filter.bySuperClass(classOf[IndexedSeq[Any]], new ArrayBuffer[Any]) must beSome
    }

    "filter when invoked with IndexedSeq and ListBuffer" in {
      Filter.bySuperClass(classOf[IndexedSeq[Any]], new ListBuffer[Any]) must beNone
    }

    "not filter when invoked with Int and 1" in {
      Filter.bySuperClass(classOf[Int], 1) must beSome
    }

    "filter when invoked with Int and 1L" in {
      Filter.bySuperClass(classOf[Int], 1L) must beNone
    }

    "not filter when invoked with Any and 1" in {
      Filter.bySuperClass(classOf[Any], 1) must beSome
    }

    "not filter when invoked with AnyVal and 1" in {
      Filter.bySuperClass(classOf[AnyVal], 1) must beSome
    }

  }
  
  "Filter.byClass" should {
    "not filter when invoked with ListBuffer and ListBuffer" in {
      Filter.byClass(classOf[ListBuffer[Any]], new ListBuffer[Any]) must beSome
    }

    "filter when invoked with IndexedSeq and ArrayBuffer" in {
      Filter.byClass(classOf[IndexedSeq[Any]], new ArrayBuffer[Any]) must beNone
    }

    "filter when invoked with IndexedSeq and ListBuffer" in {
      Filter.byClass(classOf[IndexedSeq[Any]], new ListBuffer[Any]) must beNone
    }

    "not filter when invoked with Int and 1" in {
      Filter.byClass(classOf[Int], 1) must beSome
    }

    "filter when invoked with Int and 1L" in {
      Filter.byClass(classOf[Int], 1L) must beNone
    }

    "filter when invoked with Any and 1" in {
      Filter.byClass(classOf[Any], 1) must beNone
    }

    "filter when invoked with AnyVal and 1" in {
      Filter.byClass(classOf[AnyVal], 1) must beNone
    }

  }
}