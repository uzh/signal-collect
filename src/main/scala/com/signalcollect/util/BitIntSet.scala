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

class LongBitSet(val bits: Long) extends AnyVal {

  /**
   * Returns a new LongBitSet in which that bit is set.
   */
  def set(i: Int): Long = {
    val mask = 1 << i
    println(s"|||||||||||| going from ${bits.toBinaryString} to ${(bits | mask).toBinaryString}")
    bits | mask
  }

  def foreach(f: Int => Unit) {
    if (bits != 0) {
      var next = 0
      var b = bits
      do {
        val trailing = java.lang.Long.numberOfTrailingZeros(bits)
        if (trailing != 64) {
          next += trailing
          f(next)
          val delta = trailing + 1
          next += delta
          b = b >>> delta
        }
      } while (b != 0)
    }
  }

  def foreachWithBaseValue(f: Int => Unit, baseValue: Int) {
    if (bits != 0) {
      var next = baseValue
      var b = bits
      do {
        val trailing = java.lang.Long.numberOfTrailingZeros(bits)
        if (trailing != 64) {
          next += trailing
          f(next)
          val delta = trailing + 1
          next += delta
          b = b >>> delta
        }
      } while (b != 0)
    }
  }

  def min = {
    val m = java.lang.Long.numberOfTrailingZeros(bits)
    if (m < 64) {
      m
    } else {
      throw new Exception("This set has no minimum.")
    }
  }

  def minWithBaseValue(baseValue: Int): Int = {
    min + baseValue
  }

  def max = {
    val m = 64 - java.lang.Long.numberOfLeadingZeros(bits)
    if (m < 64) {
      m
    } else {
      throw new Exception("This set has no maximum.")
    }
  }

  def maxWithBaseValue(baseValue: Int): Int = {
    max + baseValue
  }

  def toBuffer: Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    foreach(buffer.append(_))
    buffer
  }

  def toList: List[Int] = toBuffer.toList
  def toSet: Set[Int] = toBuffer.toSet

  def toBufferWithBaseValue(baseValue: Int): Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    foreachWithBaseValue(buffer.append(_), baseValue)
    buffer
  }
  def toListWithBaseValue(baseValue: Int): List[Int] = toBufferWithBaseValue(baseValue).toList
  def toSetWithBaseValue(baseValue: Int): Set[Int] = toBufferWithBaseValue(baseValue).toSet
}

object BitIntSet {
  def create(baseId: Int): Array[Long] = {
    // Size 0
    Array(((baseId | 0l) << 32) | 0l)
  }

  def create(baseId: Int, range: Int): Array[Long] = {
    val arrayLength = (range / 64.0).floor.toInt + 1
    println("Array length = " + arrayLength)
    val a = new Array[Long](arrayLength)
    new BitIntSet(a).writeSizeAndDisplacement(0, baseId)
    a
  }
}

/**
 *
 */
final class BitIntSet(val bits: Array[Long]) extends AnyVal {

  @inline def baseId: Int = bits(0).toInt

  /**
   * Number of items stored in this set.
   */
  @inline def size: Int = (bits(0) >> 32).toInt

  @inline def writeSize(newSize: Int) {
    writeSizeAndDisplacement(newSize, baseId)
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
   */
  def min: Int = {
    var i = 1
    while (i < bits.length) {
      val l = bits(i)
      if (l != 0) {
        new LongBitSet(l).minWithBaseValue(baseId + (i - 1) * 64)
      } else {
        i += 1
      }
    }
    throw new Exception("This set has no minimum.")
  }

  /**
   * Returns the largest item contained in the set.
   */
  def max: Int = {
    var i = bits.length - 1
    while (i > 0) {
      val l = bits(i)
      if (l != 0) {
        new LongBitSet(l).maxWithBaseValue(baseId + (i - 1) * 64)
      } else {
        i -= 1
      }
    }
    throw new Exception("This set has no maximum.")
  }

  /**
   * Returns true iff item is in the set.
   */
  def contains(item: Int): Boolean = {
    val adjustedPosition = item - baseId
    if (adjustedPosition >= 0) {
      val bitArrayIndex = positionToBitsIndex(adjustedPosition)
      val l = bits(bitArrayIndex)
      val insideLongIndex = positionToInsideLongIndex(adjustedPosition)
      isBitAtLongIndexSet(insideLongIndex, l)
    } else {
      false
    }
  }

  def insert(item: Int): Boolean = {
    println(s"Hey, this is an int bit set: size = $size, baseId = $baseId, now we're inserting $item")
    val adjustedPosition = item - baseId
    assert(adjustedPosition >= 0)
    val bitArrayIndex = positionToBitsIndex(adjustedPosition)
    println(s"bit array index for $item = $bitArrayIndex")
    val longBitSet = new LongBitSet(bits(bitArrayIndex))
    val insideLongIndex = positionToInsideLongIndex(adjustedPosition)
    println(s"inside long index for $item = $insideLongIndex")
    val updated = longBitSet.set(insideLongIndex)
    println(s"writing to position $insideLongIndex")
    val alreadySet = longBitSet.bits == updated
    if (!alreadySet) {
      println("wasn't set yet, doing just that")
      writeSize(size + 1)
      bits(bitArrayIndex) = updated
      println(s"that long has now ${java.lang.Long.bitCount(updated)} bits set")
      //println(s"After insert of $item: ${toSet}")
      //foreach(println(_))
      true
    } else {
      println("that item was already contained")
      false
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
    var currentBaseId = baseId
    var i = 1
    while (i < bits.length) {
      val longBitSet = new LongBitSet(bits(i))
      if (longBitSet.bits != 0) {
        longBitSet.foreachWithBaseValue(f, currentBaseId)
      }
      i += 1
      currentBaseId += 64
    }
  }

}
