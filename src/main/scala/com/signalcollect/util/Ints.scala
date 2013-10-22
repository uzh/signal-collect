package com.signalcollect.util

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Arrays

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

  implicit class SearchableIntSet(sorted: Array[Int]) extends Traversable[Int] {

    /**
     * Checks if `item` is contained by using binary search.
     */
    def contains(item: Int): Boolean = {
      //  Arrays.binarySearch(childDeltasOptimized, toFind) >= 0
      var lower = 0
      var upper = sorted.length - 1
      while (lower <= upper) {
        val mid = lower + (upper - lower) / 2
        val midItem = sorted(mid)
        if (midItem < item) {
          lower = mid + 1
        } else if (midItem > item) {
          upper = mid - 1
        } else {
          return true
        }
      }
      false
    }

  }

  implicit class IntSet(encoded: Array[Byte]) extends Traversable[Int] {

    /**
     * Adds an integer to the set and returns the updated set.
     *
     */
    def add(i: Int): Array[Byte] = {

      Array(1)
    }

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
}
