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

  def createCompactSet(ints: Array[Int]): Array[Byte] = {
    val sorted = ints.sorted
    var i = 0
    var previous = -1
    while (i < sorted.length) {
      val tmp = sorted(i)
      // TODO: Investigate encoding of 0, improve if possible.
      sorted(i) = sorted(i) - previous - 1
      previous = tmp
      i += 1
    }
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)
    i = 0
    while (i < sorted.length) {
      writeUnsignedVarInt(sorted(i), dos)
      i += 1
    }
    dos.flush
    baos.flush
    baos.toByteArray
  }

  private[signalcollect] val hasAnotherByte = Integer.parseInt("10000000", 2)
  private[signalcollect] val leastSignificant7BitsMask = Integer.parseInt("01111111", 2)
  private[signalcollect] val everythingButLeastSignificant7Bits = ~leastSignificant7BitsMask

  // Same as https://developers.google.com/protocol-buffers/docs/encoding
  def writeUnsignedVarInt(i: Int, out: DataOutputStream) {
    var remainder = i
    // While this is not the last byte, write one bit to indicate if the
    // next byte is part of this number and 7 bytes of the number itself.
    while ((remainder & everythingButLeastSignificant7Bits) != 0) {
      // First bit of byte indicates that the next byte is still part of this number, if set.
      out.writeByte((remainder & leastSignificant7BitsMask) | hasAnotherByte)
      remainder >>>= 7
    }
    // Final byte.
    out.writeByte(remainder & 0x7F)
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

}
