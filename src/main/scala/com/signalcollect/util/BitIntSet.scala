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
import scala.collection.mutable.ArrayBuffer
import java.util.BitSet
import scala.annotation.tailrec

object BitIntSet {
  def create(smallestValue: Int): Array[Long] = {
    // Size 1
    Array(((smallestValue | 0l) << 32) | 1l)
  }

  def create(smallestValue: Int, range: Int): Array[Long] = {
    val arrayLength = (range / 64.0).floor.toInt + 1
    println("Array length = " + arrayLength)
    val a = new Array[Long](arrayLength)
    new BitIntSet(a).writeSizeAndDisplacement(1, smallestValue)
    a
  }
}

/**
 *
 */
final class BitIntSet(val bits: Array[Long]) extends AnyVal {

  @inline def displacement: Int = bits(0).toInt

  /**
   * Number of items stored in this set.
   */
  @inline def size: Int = (bits(0) >> 32).toInt

  @inline def writeSize(newSize: Int) {
    writeSizeAndDisplacement(newSize, displacement)
  }

  @inline def writeDisplacement(newDisplacement: Int) {
    writeSizeAndDisplacement(size, newDisplacement)
  }

  @inline def writeSizeAndDisplacement(newSize: Int, newDisplacement: Int) {
    bits(0) = ((newSize | 0l) << 32) | (newDisplacement & 0x00000000FFFFFFFFL)
  }

  def toBuffer: Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    foreach(buffer.append(_))
    buffer
  }

  def toList: List[Int] = toBuffer.toList
  def toSet: Set[Int] = toBuffer.toSet

  /**
   * Returns the smallest item contained in the set.
   * If the set is empty returns -1.
   */
  def min: Int = displacement

  /**
   * Returns the largest item contained in the set.
   * If the set is empty returns -1.
   */
  def max: Int = ???

  /**
   * Returns true iff item is in the set.
   */
  def contains(item: Int): Boolean = {
    val adjustedPosition = item - displacement
    if (adjustedPosition > 0) {
      val bitArrayIndex = positionToBitsIndex(adjustedPosition)
      val l = bits(bitArrayIndex)
      val insideLongIndex = positionToInsideLongIndex(adjustedPosition)
      isBitAtLongIndexSet(insideLongIndex, l)
    } else if (item == 0) {
      true
    } else {
      false
    }
  }

  def insert(item: Int): Boolean = {
    println(s"Hey, this is an int bit set: size = $size, displacement = $displacement, now we're inserting $item")
    val adjustedPosition = item - displacement
    assert(adjustedPosition >= 0)
    val index = adjustedPosition - 1
    if (adjustedPosition == 0) {
      println("hah, you're adding the min element again")
      false
    } else {
      val bitArrayIndex = positionToBitsIndex(index)
      println(s"bit array index for $item = $bitArrayIndex")
      val l = bits(bitArrayIndex)
      val insideLongIndex = positionToInsideLongIndex(index)
      println(s"inside long index for $item = $insideLongIndex")
      val mask = 1l << insideLongIndex
      println(s"writing to position $insideLongIndex")
      val alreadySet = (mask & l) != 0l
      if (!alreadySet) {
        println("wasn't set yet, doing just that")
        writeSize(size + 1)
        bits(bitArrayIndex) = l | mask
        println(s"that long has now ${java.lang.Long.bitCount(l | mask)} bits set")
        //println(s"After insert of $item: ${toSet}")
        //foreach(println(_))
        true
      } else {
        println("that item was already contained")
        false
      }
    }
  }

  /**
   * We need 6 bits to index the inside of a long.
   */
  @inline def positionToBitsIndex(pos: Int): Int = {
    (pos >> 6) + 1
  }

  /**
   * Only the first 6 bits.
   */
  @inline def positionToInsideLongIndex(pos: Int): Int = {
    pos & 63
  }

  @inline def isBitAtLongIndexSet(index: Int, l: Long): Boolean = {
    val mask = 1l << index
    (mask & l) != 0l
  }

  def foreach(f: Int => Unit) {
    var currentBaseId = displacement
    var i = 1
    //println(s"bits array: " + bits.map(_.toBinaryString).mkString(""))
    val maxIndex = bits.length
    var currentId = currentBaseId
    var currentLong = bits(i)
    f(currentId)
    currentId += 1
    while (i < maxIndex) {
      val shift = -1l << currentId
      println(s"shift = $shift, has ${java.lang.Long.numberOfTrailingZeros(shift)} trailing zeros")
      val nextPosition = currentLong & shift
      println("nextPosition = " + nextPosition.toBinaryString + s" has ${java.lang.Long.numberOfTrailingZeros(nextPosition)} trailing zeros")
      if (nextPosition != 0) {
        currentId = currentBaseId + java.lang.Long.numberOfTrailingZeros(nextPosition)
        f(currentId)
        currentId += 1
      } else {
        i += 1
        if (i < maxIndex) {
          currentLong = bits(i)
          currentBaseId += 64
          currentId = currentBaseId
        }
      }
    }

    //      val nextPositions = bits(i)
    //      foreach(f, currentBaseId, nextPositions)
    //      currentBaseId += 64
    //      i += 1
    //  @tailrec @inline def foreach(f: Int => Unit, baseId: Int, nextPositions: Long) {
    //    var next: Long = baseId
    //    l >>> 63
    //    if (nextPositions != 0) {
    //      val relevant = -1l << nextPositions
    //      val trailing = java.lang.Long.numberOfTrailingZeros(nextPositions)
    //      //println(s"delta = $delta, nextPositions = $nextPositions, baseId = $baseId, nextPos after shift = ${nextPositions >>> (delta + 1)}")
    //      val nextInt = baseId + trailing
    //      f(nextInt)
    //      foreach(f, nextInt + 1, nextPositions >>> trailing)
    //    }
  }

}
