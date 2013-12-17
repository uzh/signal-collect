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

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import com.signalcollect.util.Ints._

class IntSet(val encoded: Array[Byte]) extends AnyVal {

  def toBuffer: Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    foreach(buffer.append(_))
    buffer
  }

  def toList: List[Int] = toBuffer.toList
  def toSet: Set[Int] = toBuffer.toSet

  /**
   * Returns true iff item is in the set.
   */
  def contains(item: Int): Boolean = findIndex(item) >= 0

  /**
   * Inserts item into a variable length and delta encoded int set.
   * Determines insert index, computes extra space, splices in inserted element, adjusts next delta
   * and uses arraycopy twice to perform insert.
   * If the item was contained already, returns the same array again (reference equal).
   */
  def insert(item: Int): Array[Byte] = {
    var i = 0
    var previousInt = -1
    var startingIndexOfCurrentDelta = 0
    var currentDecodedInt = 0
    var shift = 0
    // Inlining manually to ensure there is no boxing.
    while (i < encoded.length) {
      val readByte = encoded(i)
      currentDecodedInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentDecodedInt + 1
        if (previousInt == item) {
          // Item already contained, return old encoded array.
          return encoded
        } else if (previousInt > item) {
          // The delta we have to encode for the newly inserted item.
          val itemDelta = currentDecodedInt - (previousInt - item)
          val itemAfterItemDelta = (previousInt - item) - 1
          val bytesForItem = bytesForVarint(itemDelta)
          val bytesForNextBeforeInsert = bytesForVarint(currentDecodedInt)
          val bytesForNextAfterInsert = bytesForVarint(itemAfterItemDelta)
          val extraBytesRequired = (bytesForItem + bytesForNextAfterInsert) - bytesForNextBeforeInsert
          val newEncoded = new Array[Byte](encoded.length + extraBytesRequired)
          // Copy old encoded up to insertion index.
          System.arraycopy(encoded, 0, newEncoded, 0, startingIndexOfCurrentDelta)
          writeUnsignedVarInt(itemDelta, newEncoded, startingIndexOfCurrentDelta)
          val itemAfterItemDeltaIndex = startingIndexOfCurrentDelta + bytesForItem
          writeUnsignedVarInt(itemAfterItemDelta, newEncoded, itemAfterItemDeltaIndex)
          val secondCopyBytes = encoded.length - startingIndexOfCurrentDelta - bytesForNextBeforeInsert
          System.arraycopy(encoded, startingIndexOfCurrentDelta + bytesForNextBeforeInsert, newEncoded, itemAfterItemDeltaIndex + bytesForNextAfterInsert, secondCopyBytes)
          return newEncoded
        }
        currentDecodedInt = 0
        shift = 0
        startingIndexOfCurrentDelta = i + 1
      }
      i += 1
    }
    // Insert at the end of the array.
    val itemDelta = (item - previousInt) - 1
    val extraBytesRequired = bytesForVarint(itemDelta)
    val newEncoded = new Array[Byte](encoded.length + extraBytesRequired)
    // Copy old encoded up to insertion index.
    System.arraycopy(encoded, 0, newEncoded, 0, startingIndexOfCurrentDelta)
    writeUnsignedVarInt(itemDelta, newEncoded, i)
    return newEncoded
  }

  /**
   * Returns the index of the first byte of element
   * item, iff item is contained in the set.
   * Returns -1 otherwise.
   */
  private def findIndex(item: Int): Int = {
    val notFound = -1
    var i = 0
    var previousInt = -1
    var startingIndexOfCurrentInt = 0
    var currentInt = 0
    var shift = 0
    // Inlining manually to ensure there is no boxing.
    while (i < encoded.length) {
      val readByte = encoded(i)
      currentInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentInt + 1
        if (previousInt == item) {
          return startingIndexOfCurrentInt
        } else if (previousInt > item) {
          return notFound
        }
        currentInt = 0
        shift = 0
        startingIndexOfCurrentInt = i + 1
      }
      i += 1
    }
    notFound
  }

  @inline def foreach(f: Int => Unit) {
    var i = 0
    var previousInt = -1
    var currentInt = 0
    var shift = 0
    while (i < encoded.length) {
      val readByte = encoded(i)
      currentInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentInt + 1
        f(previousInt)
        currentInt = 0
        shift = 0
      }
      i += 1
    }
  }
}
