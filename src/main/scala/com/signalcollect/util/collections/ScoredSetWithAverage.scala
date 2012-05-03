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

import java.util.LinkedHashMap
import collection.JavaConversions._
import java.util.LinkedHashMap.Entry

/**
 * A class that supports storing sets with items that have associated scores.
 *
 * The average score is computed incrementally over changes to the set.
 * There is also a set traversal that visits items in an order such that
 * each item traversed item is scored equal to or above the average of the entire set.
 */
class ScoredSetWithAverage[ItemType] {

  protected val implementation: LinkedHashMap[ItemType, Double] = new LinkedHashMap[ItemType, Double]()

  protected var averageScore = 0.0

  protected var entrySetIterator = implementation.entrySet.iterator

  def isEmpty = implementation.isEmpty

  def contains(item: ItemType): Boolean = {
    implementation.containsKey(item)
  }

  def getScore(item: ItemType): Double = {
    implementation.get(item)
  }

  def size = implementation.size

  def getAverage = averageScore

  def foreach[U](f: (Any) => U) = {
    implementation.keySet.foreach(f)
  }

  def updateItemScore(item: ItemType, newScore: Double) {
    val oldScore = implementation.get(item)
    implementation.put(item, newScore)
    averageScore = averageScore + (newScore - oldScore) / implementation.size
  }

  def add(item: ItemType, score: Double) {
    implementation.put(item, score)
    averageScore = averageScore + (score - averageScore) / implementation.size
  }

  def remove(item: ItemType): Unit = {
    val score = implementation.get(item)
    implementation.remove(item)
    if (implementation.size == 0) {
      averageScore = 0.0
    } else {
      averageScore = averageScore - (score - averageScore) / implementation.size
    }
  }

  protected def nextEntry = {
    if (entrySetIterator.hasNext) {
      entrySetIterator.next
    } else {
      entrySetIterator = implementation.entrySet.iterator
      entrySetIterator.next
    }
  }

  protected def recomputeAverage {
    averageScore = implementation.values.sum
  }

  def nextAboveAverageItem: ItemType = {
    var entry = nextEntry
    while (entry.getValue < averageScore) {
      entry = nextEntry
    }
    val item = entry.getKey
    item
  }

}