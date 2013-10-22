package com.signalcollect.util

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Utility for encoding/decoding unsigned variable length integers.
 */
object Ints {
  def create(ints: Array[Int]): Array[Byte] = {
    val sorted = ints.sorted
    var i = 0
    var previous = 0
    while (i < sorted.length) {
      val tmp = sorted(i)
      sorted(i) = sorted(i) - previous - 1
      previous = tmp
      i += 1
    }
    val baos = new ByteArrayOutputStream()
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

  private val hasAnotherByte = Integer.parseInt("10000000", 2)
  private val leastSignificant7BitsMask = Integer.parseInt("01111111", 2)
  private val everythingButLeastSignificant7Bits = ~leastSignificant7BitsMask

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

  implicit class IntSet(encoded: Array[Byte]) extends Traversable[Int] {
    def foreach[U](f: Int => U) {
      var i = 0
      var previousInt = 0
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
}
