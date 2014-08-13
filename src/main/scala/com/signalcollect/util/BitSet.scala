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

import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec

class BasicBitSet(val bits: Long) extends AnyVal {

  /**
   * Returns a new BasicBitSet in which that bit is set.
   */
  @inline final def set(i: Int): Long = {
    val mask = 1l << i
    bits | mask
  }

  /**
   * Returns true iff this set contains i.
   */
  @inline final def contains(i: Int): Boolean = {
    val mask = 1l << i
    (bits & mask) != 0
  }

  @inline final def size: Int = {
    java.lang.Long.bitCount(bits)
  }

  @inline final def foreach(f: Int => Unit) {
    if (bits != 0) {
      var next = 0
      var b = bits
      do {
        val trailing = java.lang.Long.numberOfTrailingZeros(b)
        f(next + trailing)
        val shift = trailing + 1
        b >>>= shift
        next += shift
      } while (b != 0 && next < 64)
    }
  }

  @inline final def foreachWithBaseValue(f: Int => Unit, baseValue: Int) {
    if (bits != 0) {
      val max = 64
      var next = 0
      var b = bits
      do {
        val trailing = java.lang.Long.numberOfTrailingZeros(b)
        f(next + trailing + baseValue)
        val shift = trailing + 1
        b >>>= shift
        next += shift
      } while (b != 0 && next < max)
    }
  }

  @inline final def min = {
    java.lang.Long.numberOfTrailingZeros(bits)
  }

  @inline final def minWithBaseValue(baseValue: Int): Int = {
    min + baseValue
  }

  @inline final def max = {
    63 - java.lang.Long.numberOfLeadingZeros(bits)
  }

  @inline final def maxWithBaseValue(baseValue: Int): Int = {
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

object BitSet {

  def apply(a: Array[Int]): Array[Long] = {
    assert(a.nonEmpty)
    val min = a.min
    val max = a.max
    val range = math.abs(max - min) + 1
    assert(range > 0)
    val r = create(min, range)
    for (i <- a) {
      new BitSet(r).insert(i)
    }
    r
  }

  def create(baseId: Int, range: Int): Array[Long] = {
    val arrayLength = (range / 64.0).ceil.toInt + 1
    val a = new Array[Long](arrayLength)
    new BitSet(a).writeSizeAndDisplacement(0, baseId)
    a
  }
}

/**
 *
 */
final class BitSet(val bits: Array[Long]) extends AnyVal {

  @inline final def baseId: Int = bits(0).toInt

  /**
   * Number of items stored in this set.
   */
  @inline final def size: Int = (bits(0) >> 32).toInt

  @inline final def writeSize(newSize: Int) {
    writeSizeAndDisplacement(newSize, baseId)
  }

  @inline final def writeDisplacement(newDisplacement: Int) {
    writeSizeAndDisplacement(size, newDisplacement)
  }

  final def writeSizeAndDisplacement(newSize: Int, newDisplacement: Int) {
    bits(0) = ((newSize | 0l) << 32) | (newDisplacement & 0x00000000FFFFFFFFL)
  }

  override def toString: String = {
    s"(size = $size, baseId = $baseId: ${bits.tail.map(_.toBinaryString).mkString(",")})"
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
  @inline final def min: Int = {
    var i = 1
    while (i < bits.length) {
      val l = bits(i)
      if (l != 0) {
        return new BasicBitSet(l).minWithBaseValue(baseId + (i - 1) * 64)
      } else {
        i += 1
      }
    }
    throw new Exception("This set has no minimum.")
  }

  /**
   * Returns the largest item contained in the set.
   */
  @inline final def max: Int = {
    var i = bits.length - 1
    while (i > 0) {
      val l = bits(i)
      if (l != 0) {
        return new BasicBitSet(l).maxWithBaseValue(baseId + (i - 1) * 64)
      } else {
        i -= 1
      }
    }
    throw new Exception("This set has no maximum.")
  }

  /**
   * Returns true iff item is in the set.
   */
  @inline final def contains(item: Int): Boolean = {
    val adjustedPosition = item - baseId
    if (adjustedPosition >= 0) {
      val bitArrayIndex = positionToBitsIndex(adjustedPosition)
      if (bitArrayIndex >= 1 && bitArrayIndex < bits.length) {
        val l = bits(bitArrayIndex)
        val insideLongIndex = positionToInsideLongIndex(adjustedPosition)
        isBitAtLongIndexSet(insideLongIndex, l)
      } else {
        false
      }
    } else {
      false
    }
  }

  @inline final def insert(item: Int): Boolean = {
    val adjustedPosition = item - baseId
    assert(adjustedPosition >= 0)
    val bitArrayIndex = positionToBitsIndex(adjustedPosition)
    val longBitSet = new BasicBitSet(bits(bitArrayIndex))
    val insideLongIndex = positionToInsideLongIndex(adjustedPosition)
    val updated = longBitSet.set(insideLongIndex)
    val alreadySet = longBitSet.bits == updated
    if (!alreadySet) {
      writeSize(size + 1)
      bits(bitArrayIndex) = updated
      true
    } else {
      false
    }
  }

  /**
   * We need 6 bits to index the inside of a long.
   */
  @inline final def positionToBitsIndex(pos: Int): Int = {
    (pos >> 6) + 1
  }

  /**
   * Only the first 6 bits.
   */
  @inline final def positionToInsideLongIndex(pos: Int): Int = {
    pos & 63
  }

  @inline final def isBitAtLongIndexSet(index: Int, l: Long): Boolean = {
    val mask = 1l << index
    (mask & l) != 0l
  }

  @inline final def foreach(f: Int => Unit) {
    var currentBaseId = baseId
    var i = 1
    while (i < bits.length) {
      new BasicBitSet(bits(i)).foreachWithBaseValue(f, currentBaseId)
      i += 1
      currentBaseId += 64
    }
  }

}
