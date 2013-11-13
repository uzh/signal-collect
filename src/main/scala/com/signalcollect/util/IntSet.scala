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

import com.signalcollect.util.Ints.hasAnotherByte
import com.signalcollect.util.Ints.leastSignificant7BitsMask
import com.signalcollect.util.Ints.writeUnsignedVarInt

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
   * Inserts item into the set.
   */
  def insert(item: Int): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)
    var itemPresentOrWritten = false
    var previous = -1
    foreach(current => {
      if (!itemPresentOrWritten) {
        if (current < item) {
        } else if (current == item) {
          itemPresentOrWritten = true
        } else if (current > item) {
          writeUnsignedVarInt(item - previous - 1, dos)
          previous = item
          itemPresentOrWritten = true
        }
      }
      writeUnsignedVarInt(current - previous - 1, dos)
      previous = current
    })
    if (!itemPresentOrWritten) {
      writeUnsignedVarInt(item - previous - 1, dos)
    }
    dos.flush
    baos.flush
    baos.toByteArray
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

  def foreach[U](f: Int => U) {
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
