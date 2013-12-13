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

/**
 * Variant of IntSet that maintains extra space at the end of
 * the array in order to allow for faster inserts.
 * It also stores the number of items in the set as the first varint
 * entry of the encoded array.
 * The number of used bytes is variable-length encoded reading from
 * the end of the array backwards. In all other aspects the set
 * is the same, using variable-length delta encoding to compactly store
 * sets of ints.
 */
class FastInsertIntSet(val encoded: Array[Byte]) extends AnyVal {

  def toBuffer: Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    foreach(buffer.append(_))
    buffer
  }

  def toList: List[Int] = toBuffer.toList
  def toSet: Set[Int] = toBuffer.toSet

  def freeBytes = readUnsignedVarIntBackwards(encoded, encoded.length - 1)

  def totalBytes = encoded.length

  //  /**
  //   * Number of bytes that are dedicated to encoding the set items.
  //   * Returns true iff item is in the set.
  //   */
  //  def setEncodingBytes = {
  //    val free = freeBytes
  //    totalBytes - free - bytesForVarint(free)
  //  }

  /**
   * Number of items.
   */
  def size: Int = readUnsignedVarInt(encoded, 0)

  /**
   * Returns true iff item is in the set.
   */
  def contains(item: Int): Boolean = findIndex(item) >= 0

  /**
   * Inserts item into a variable length and delta encoded int set.
   * Determines insert index, computes extra space, splices in inserted element, adjusts next delta
   * and uses arraycopy twice to perform insert.
   * Returns an array that contains the inserted item.
   */
  def insert(item: Int, overheadFraction: Float = 0.2f): Array[Byte] = {
    val numberOfIntsBeforeInsert = size
    val sizeOfSizeEntry = bytesForVarint(size)
    // Shift starting point by number of bytes spent on encoding the size.
    // TODO: Create test case for the special case where the size changes.
    var i = sizeOfSizeEntry
    var intsTraversedSoFar = 0
    var previousInt = -1
    var startingIndexOfCurrentDelta = i
    var currentDecodedInt = 0
    var shift = 0
    while (intsTraversedSoFar < numberOfIntsBeforeInsert) {
      val readByte = encoded(i)
      currentDecodedInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentDecodedInt + 1
        intsTraversedSoFar += 1
        if (previousInt == item) {
          // Item already contained, return old encoded array.
          return encoded
        } else if (previousInt > item) {
          val updatedEncoded = handleInsert(
            item,
            false,
            startingIndexOfCurrentDelta,
            currentDecodedInt,
            previousInt,
            numberOfIntsBeforeInsert,
            sizeOfSizeEntry,
            overheadFraction)
          return updatedEncoded
        }
        currentDecodedInt = 0
        shift = 0
        startingIndexOfCurrentDelta = i + 1
      }
      i += 1
    }
    val updatedEncoded = handleInsert(
      item,
      true,
      startingIndexOfCurrentDelta,
      currentDecodedInt,
      previousInt,
      numberOfIntsBeforeInsert,
      sizeOfSizeEntry,
      overheadFraction)
    //    // Insert at the end of the array.
    //    val itemDelta = (item - previousInt) - 1
    //    val extraBytesRequired = bytesForVarint(itemDelta)
    //    val newEncoded = new Array[Byte](encoded.length + extraBytesRequired)
    //    // Copy old encoded up to insertion index.
    //    System.arraycopy(encoded, 0, newEncoded, 0, startingIndexOfCurrentDelta)
    //    writeUnsignedVarInt(itemDelta, newEncoded, i)
    return updatedEncoded
  }

  /**
   * I'm aware that this code is hard to understand and not sure how to
   * fix that without sacrificing performance.
   */
  private def handleInsert(
    item: Int,
    lastItemInSet: Boolean,
    insertIndexBeforeSizeAdjustment: Int,
    lastDecodedDelta: Int,
    lastDecodedInt: Int,
    numberOfIntsBeforeInsert: Int,
    sizeOfSizeEntry: Int,
    overheadFraction: Float): Array[Byte] = {
    var insertIndex = insertIndexBeforeSizeAdjustment
    val numberOfIntsAfterInsert = numberOfIntsBeforeInsert + 1
    val bytesForSizeBeforeInsert = bytesForVarint(numberOfIntsBeforeInsert)
    val bytesForSizeAfterInsert = bytesForVarint(numberOfIntsAfterInsert)
    val extraBytesForSize = bytesForSizeAfterInsert - bytesForSizeBeforeInsert
    // The delta we have to encode for the newly inserted item.
    val itemDelta = lastDecodedDelta - (lastDecodedInt - item)
    val itemAfterItemDelta = (lastDecodedInt - item) - 1
    val bytesForItem = bytesForVarint(itemDelta)
    val bytesForNextBeforeInsert = bytesForVarint(lastDecodedDelta)
    val bytesForNextAfterInsert = bytesForVarint(itemAfterItemDelta)
    val extraBytesForNextAfterInsert = bytesForNextAfterInsert - bytesForNextBeforeInsert
    val freeBytesBeforeInsert = freeBytes
    val extraBytesRequired = extraBytesForSize + bytesForItem + extraBytesForNextAfterInsert
    val freeBytesAfterInsert = freeBytesBeforeInsert - extraBytesRequired
    // +1 extra byte to encode that there are 0 free bytes.
    var targetArray = encoded
    if (freeBytesBeforeInsert < extraBytesRequired + 1) {
      // We need to allocate a larger array.
      targetArray = allocateNewArray(extraBytesRequired, overheadFraction)
    }
    if (bytesForSizeAfterInsert > sizeOfSizeEntry) {
      // The size of the number of entries encoding has changed, we need to shift everything forward.
      val encodedBytesUpToInsertPosition = insertIndex - sizeOfSizeEntry
      System.arraycopy(encoded, sizeOfSizeEntry, targetArray, bytesForSizeAfterInsert, encodedBytesUpToInsertPosition)
      // Update starting index of current delta by the extra byte for the increased size.
      insertIndex += 1
    }
    val firstFreeByteBeforeInsert = encoded.length - freeBytesBeforeInsert
    val secondCopyBytes = firstFreeByteBeforeInsert - insertIndexBeforeSizeAdjustment - bytesForNextBeforeInsert
    val itemAfterItemDeltaIndexAfterInsert = insertIndex + bytesForItem
    writeUnsignedVarInt(numberOfIntsAfterInsert, targetArray, 0)
    writeUnsignedVarInt(itemDelta, targetArray, insertIndex)
    writeUnsignedVarInt(itemAfterItemDelta, targetArray, itemAfterItemDeltaIndexAfterInsert)
    writeUnsignedVarIntBackwards(freeBytesAfterInsert, targetArray, targetArray.length - 1)
    System.arraycopy(encoded, insertIndexBeforeSizeAdjustment + bytesForNextBeforeInsert, targetArray, itemAfterItemDeltaIndexAfterInsert + bytesForNextAfterInsert, secondCopyBytes)
    targetArray
  }

  private def allocateNewArray(extraBytesRequired: Int, overheadFraction: Float): Array[Byte] = {
    val minRequiredLength = encoded.length.toLong + extraBytesRequired + 1
    if (minRequiredLength > Int.MaxValue) {
      throw new Exception(
        s"Could not allocate sufficiently large array to back FastInsertIntSet (required size: $minRequiredLength).")
    }
    val newDesiredlength = minRequiredLength * (1.0 + overheadFraction).ceil
    // If the desired length is too large, go with Int.MaxValue.
    val newEncodedLength = math.min(Int.MaxValue, newDesiredlength).toInt
    val newEncoded = new Array[Byte](newEncodedLength)
    newEncoded
  }

  /**
   * Returns the index of the first byte of element
   * item, iff item is contained in the set.
   * Returns -1 otherwise.
   */
  private def findIndex(item: Int): Int = {
    val intsTotal = size
    var intsTraversedSoFar = 0
    // Shift starting point by number of bytes spent on encoding the size.
    var i = bytesForVarint(size)
    var previousInt = -1
    var startingIndexOfCurrentInt = 0
    var currentInt = 0
    var shift = 0
    while (intsTraversedSoFar < intsTotal) {
      val readByte = encoded(i)
      currentInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentInt + 1
        intsTraversedSoFar += 1
        if (previousInt == item) {
          return startingIndexOfCurrentInt
        } else if (previousInt > item) {
          val notFound = -1
          return notFound
        }
        currentInt = 0
        shift = 0
        startingIndexOfCurrentInt = i + 1
      }
      i += 1
    }
    val notFound = -1
    return notFound
  }

  @inline def foreach(f: Int => Unit) {
    val intsTotal = size
    var intsTraversedSoFar = 0
    // Shift starting point by number of bytes spent on encoding the size.
    var i = bytesForVarint(size)
    var previousInt = -1
    var currentInt = 0
    var shift = 0
    while (intsTraversedSoFar < intsTotal) {
      val readByte = encoded(i)
      currentInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
      if ((readByte & hasAnotherByte) == 0) {
        // Next byte is no longer part of this Int.
        previousInt += currentInt + 1
        f(previousInt)
        intsTraversedSoFar += 1
        currentInt = 0
        shift = 0
      }
      i += 1
    }
  }
}
