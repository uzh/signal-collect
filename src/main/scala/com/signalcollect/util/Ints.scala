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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Arrays
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import com.signalcollect.util.Ints._

/**
 * Utility for encoding/decoding unsigned variable length integers.
 */
object Ints {

  def createSearchableSet(ints: Array[Int]): Array[Int] = {
    // TODO: Make more efficient by sorting first, then filtering the sorted array.
    val distinct = ints.distinct
    Arrays.sort(distinct)
    distinct
  }

  /**
   * Modifies the input array.
   */
  def createCompactSet(ints: Array[Int]): Array[Byte] = {
    java.util.Arrays.sort(ints)
    var i = 0
    var previous = -1
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)
    while (i < ints.length) {
      val tmp = ints(i)
      writeUnsignedVarInt(tmp - previous - 1, dos)
      previous = tmp
      i += 1
    }
    i = 0
    dos.flush
    baos.flush
    baos.toByteArray
  }

  def createEmptyFastInsertIntSet: Array[Byte] = {
    // 0 entries and 1 free byte at the end that encodes that there is 1 free byte at the end.
    Array(0, 1)
  }

  private[signalcollect] val hasAnotherByte = Integer.parseInt("10000000", 2)
  private[signalcollect] val leastSignificant7BitsMask = Integer.parseInt("01111111", 2)
  private[signalcollect] val everythingButLeastSignificant7Bits = ~leastSignificant7BitsMask

  def bytesForVarint(v: Int): Int = {
    var bs = 1
    var m = v
    while ((m & everythingButLeastSignificant7Bits) != 0) {
      bs += 1
      m >>>= 7
    }
    bs
  }

  // Same as https://developers.google.com/protocol-buffers/docs/encoding
  def writeUnsignedVarInt(item: Int, out: DataOutputStream) {
    var remainder = item
    // While this is not the last byte, write one bit to indicate if the
    // next byte is part of this number and 7 bytes of the number itself.
    while ((remainder & everythingButLeastSignificant7Bits) != 0) {
      // First bit of byte indicates that the next byte is still part of this number, if set.
      out.writeByte((remainder & leastSignificant7BitsMask) | hasAnotherByte)
      remainder >>>= 7
    }
    // Final byte.
    out.writeByte(remainder)
  }

  // Write a variable length integer at a given index into an array.
  def writeUnsignedVarInt(item: Int, a: Array[Byte], index: Int) {
    var remainder = item
    var currentIndex = index
    // While this is not the last byte, write one bit to indicate if the
    // next byte is part of this number and 7 bytes of the number itself.
    while ((remainder & everythingButLeastSignificant7Bits) != 0) {
      // First bit of byte indicates that the next byte is still part of this number, if set.
      a(currentIndex) = ((remainder & leastSignificant7BitsMask) | hasAnotherByte).toByte
      currentIndex += 1
      remainder >>>= 7
    }
    // Final byte.
    a(currentIndex) = remainder.toByte
  }

  // Write a variable length integer at a given index into an array.
  // The number is written from the insert index on backwards.
  def writeUnsignedVarIntBackwards(item: Int, a: Array[Byte], index: Int) {
    var remainder = item
    var currentIndex = index
    // While this is not the last byte, write one bit to indicate if the
    // next byte is part of this number and 7 bytes of the number itself.
    while ((remainder & everythingButLeastSignificant7Bits) != 0) {
      // First bit of byte indicates that the next byte is still part of this number, if set.
      a(currentIndex) = ((remainder & leastSignificant7BitsMask) | hasAnotherByte).toByte
      currentIndex -= 1
      remainder >>>= 7
    }
    // Final byte.
    a(currentIndex) = remainder.toByte
  }

  def readUnsignedVarInt(in: DataInputStream): Int = {
    try {
      var readByte = in.readByte
      var decodedInt = readByte & leastSignificant7BitsMask
      var shift = 7
      while ((readByte & hasAnotherByte) != 0) {
        readByte = in.readByte
        decodedInt |= (readByte & leastSignificant7BitsMask) << shift
        shift += 7
      }
      decodedInt
    } catch {
      case t: Throwable => -1
    }
  }

  // Write a variable length integer at a given index into an array.
  // The number is written from the insert index on.
  @inline def readUnsignedVarInt(a: Array[Byte], index: Int): Int = {
    var currentIndex = index
    var readByte = a(currentIndex)
    var decodedInt = readByte & leastSignificant7BitsMask
    var shift = 7
    while ((readByte & hasAnotherByte) != 0) {
      currentIndex += 1
      readByte = a(currentIndex)
      decodedInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
    }
    decodedInt
  }

  // Write a variable length integer at a given index into an array.
  // The number is written from the insert index on backwards.
  @inline def readUnsignedVarIntBackwards(a: Array[Byte], index: Int): Int = {
    var currentIndex = index
    var readByte = a(currentIndex)
    var decodedInt = readByte & leastSignificant7BitsMask
    var shift = 7
    while ((readByte & hasAnotherByte) != 0) {
      currentIndex -= 1
      readByte = a(currentIndex)
      decodedInt |= (readByte & leastSignificant7BitsMask) << shift
      shift += 7
    }
    decodedInt
  }

}
