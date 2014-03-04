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

import scala.collection.mutable.Buffer

class SearchableIntSet(val sorted: Array[Int]) extends AnyVal {

  def toBuffer: Buffer[Int] = sorted.toBuffer
  def toList: List[Int] = sorted.toList
  def toSet: Set[Int] = sorted.toSet

  @inline def foreach(f: Int => Unit) = {
    var i = 0
    while (i < sorted.length) {
      f(sorted(i))
      i += 1
    }
  }

  /**
   * Inserts item into the searchable int set.
   * If the item was contained already, returns the same array again (reference equal).
   */
  def insert(item: Int): Array[Int] = {
    val index = insertIndex(item)
    if (index < sorted.length && sorted(index) == item) {
      return sorted
    } else {
      val newArray = new Array[Int](sorted.length + 1)
      System.arraycopy(sorted, 0, newArray, 0, index)
      newArray(index) = item
      System.arraycopy(sorted, index, newArray, index + 1, sorted.length - index)
      return newArray
    }
  }

  /**
   * Determines the index at which 'item' should be inserted.
   */
  def insertIndex(item: Int): Int = {
    var lower = 0
    var upper = sorted.length - 1
    while (lower <= upper) {
      val mid = lower + (upper - lower) / 2
      val midItem = sorted(mid)
      if (midItem < item) {
        lower = mid + 1
      } else if (midItem > item) {
        upper = mid - 1
      } else {
        return mid
      }
    }
    lower + (upper - lower) / 2
  }

  /**
   * Checks if `item` is contained by using binary search.
   */
  def contains(item: Int): Boolean = {
    //  Arrays.binarySearch(childDeltasOptimized, toFind) >= 0
    var lower = 0
    var upper = sorted.length - 1
    while (lower <= upper) {
      val mid = lower + (upper - lower) / 2
      val midItem = sorted(mid)
      if (midItem < item) {
        lower = mid + 1
      } else if (midItem > item) {
        upper = mid - 1
      } else {
        return true
      }
    }
    false
  }

}
