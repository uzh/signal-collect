package com.signalcollect.util

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

import com.signalcollect.util.Ints._

object FastInsertIntSetTesting {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(300); 
  println("Welcome to the Scala worksheet");$skip(45); 

  val encoded = createEmptyFastInsertIntSet;System.out.println("""encoded  : Array[Byte] = """ + $show(encoded ));$skip(76); 

  def freeBytes = readUnsignedVarIntBackwards(encoded, encoded.length - 1);System.out.println("""freeBytes: => Int""");$skip(35); 

  def totalBytes = encoded.length;System.out.println("""totalBytes: => Int""");$skip(339); 

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
  def size: Int = readUnsignedVarInt(encoded, 0);System.out.println("""size: => Int""");$skip(651); 

  def allocateNewArray(extraBytesRequired: Int, overheadFraction: Float): Array[Byte] = {
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
  };System.out.println("""allocateNewArray: (extraBytesRequired: Int, overheadFraction: Float)Array[Byte]""");$skip(16); 

  val item = 0;System.out.println("""item  : Int = """ + $show(item ));$skip(37); 
  val overheadFraction: Float = 0.2f;System.out.println("""overheadFraction  : Float = """ + $show(overheadFraction ));$skip(38); 
  val numberOfIntsBeforeInsert = size;System.out.println("""numberOfIntsBeforeInsert  : Int = """ + $show(numberOfIntsBeforeInsert ));$skip(45); 
  val sizeOfSizeEntry = bytesForVarint(size);System.out.println("""sizeOfSizeEntry  : Int = """ + $show(sizeOfSizeEntry ));$skip(172); 
  // Shift starting point by number of bytes spent on encoding the size.
  // TODO: Create test case for the special case where the size changes.
  var i = sizeOfSizeEntry;System.out.println("""i  : Int = """ + $show(i ));$skip(29); 
  var intsTraversedSoFar = 0;System.out.println("""intsTraversedSoFar  : Int = """ + $show(intsTraversedSoFar ));$skip(23); 
  var previousInt = -1;System.out.println("""previousInt  : Int = """ + $show(previousInt ));$skip(42); 
  var insertIndexBeforeSizeAdjustment = i;System.out.println("""insertIndexBeforeSizeAdjustment  : Int = """ + $show(insertIndexBeforeSizeAdjustment ));$skip(28); 
  var currentDecodedInt = 0;System.out.println("""currentDecodedInt  : Int = """ + $show(currentDecodedInt ));$skip(16); 
  var shift = 0;System.out.println("""shift  : Int = """ + $show(shift ));$skip(3279); 
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
        encoded
      } else if (previousInt > item) {
        var insertIndex = insertIndexBeforeSizeAdjustment
        val numberOfIntsAfterInsert = numberOfIntsBeforeInsert + 1
        val bytesForSizeBeforeInsert = bytesForVarint(numberOfIntsBeforeInsert)
        val bytesForSizeAfterInsert = bytesForVarint(numberOfIntsAfterInsert)
        val extraBytesForSize = bytesForSizeAfterInsert - bytesForSizeBeforeInsert
        // The delta we have to encode for the newly inserted item.
        val itemDelta = currentDecodedInt - (previousInt - item)
        val itemAfterItemDelta = (previousInt - item) - 1
        val bytesForItem = bytesForVarint(itemDelta)
        val bytesForNextBeforeInsert = bytesForVarint(currentDecodedInt)
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
      currentDecodedInt = 0
      shift = 0
      insertIndexBeforeSizeAdjustment = i + 1
    }
    i += 1
  };$skip(95); 
  // Insert at the end of the array.
        var insertIndex = insertIndexBeforeSizeAdjustment;System.out.println("""insertIndex  : Int = """ + $show(insertIndex ));$skip(67); 
        val numberOfIntsAfterInsert = numberOfIntsBeforeInsert + 1;System.out.println("""numberOfIntsAfterInsert  : Int = """ + $show(numberOfIntsAfterInsert ));$skip(80); 
        val bytesForSizeBeforeInsert = bytesForVarint(numberOfIntsBeforeInsert);System.out.println("""bytesForSizeBeforeInsert  : Int = """ + $show(bytesForSizeBeforeInsert ));$skip(78); 
        val bytesForSizeAfterInsert = bytesForVarint(numberOfIntsAfterInsert);System.out.println("""bytesForSizeAfterInsert  : Int = """ + $show(bytesForSizeAfterInsert ));$skip(83); 
        val extraBytesForSize = bytesForSizeAfterInsert - bytesForSizeBeforeInsert;System.out.println("""extraBytesForSize  : Int = """ + $show(extraBytesForSize ));$skip(133); 
        // The delta we have to encode for the newly inserted item.
        val itemDelta = currentDecodedInt - (previousInt - item);System.out.println("""itemDelta  : Int = """ + $show(itemDelta ));$skip(58); 
        val itemAfterItemDelta = (previousInt - item) - 1;System.out.println("""itemAfterItemDelta  : Int = """ + $show(itemAfterItemDelta ));$skip(53); 
        val bytesForItem = bytesForVarint(itemDelta);System.out.println("""bytesForItem  : Int = """ + $show(bytesForItem ));$skip(73); 
        val bytesForNextBeforeInsert = bytesForVarint(currentDecodedInt);System.out.println("""bytesForNextBeforeInsert  : Int = """ + $show(bytesForNextBeforeInsert ));$skip(73); 
        val bytesForNextAfterInsert = bytesForVarint(itemAfterItemDelta);System.out.println("""bytesForNextAfterInsert  : Int = """ + $show(bytesForNextAfterInsert ));$skip(94); 
        val extraBytesForNextAfterInsert = bytesForNextAfterInsert - bytesForNextBeforeInsert;System.out.println("""extraBytesForNextAfterInsert  : Int = """ + $show(extraBytesForNextAfterInsert ));$skip(46); 
        val freeBytesBeforeInsert = freeBytes;System.out.println("""freeBytesBeforeInsert  : Int = """ + $show(freeBytesBeforeInsert ));$skip(97); 
        val extraBytesRequired = extraBytesForSize + bytesForItem + extraBytesForNextAfterInsert;System.out.println("""extraBytesRequired  : Int = """ + $show(extraBytesRequired ));$skip(78); 
        val freeBytesAfterInsert = freeBytesBeforeInsert - extraBytesRequired;System.out.println("""freeBytesAfterInsert  : Int = """ + $show(freeBytesAfterInsert ));$skip(98); 
        // +1 extra byte to encode that there are 0 free bytes.
        var targetArray = encoded;System.out.println("""targetArray  : Array[Byte] = """ + $show(targetArray ));$skip(200); 
        if (freeBytesBeforeInsert < extraBytesRequired + 1) {
          // We need to allocate a larger array.
          targetArray = allocateNewArray(extraBytesRequired, overheadFraction)
        };$skip(494); 
        if (bytesForSizeAfterInsert > sizeOfSizeEntry) {
          // The size of the number of entries encoding has changed, we need to shift everything forward.
          val encodedBytesUpToInsertPosition = insertIndex - sizeOfSizeEntry
          System.arraycopy(encoded, sizeOfSizeEntry, targetArray, bytesForSizeAfterInsert, encodedBytesUpToInsertPosition)
          // Update starting index of current delta by the extra byte for the increased size.
          insertIndex += 1
        };$skip(79); 
        val firstFreeByteBeforeInsert = encoded.length - freeBytesBeforeInsert;System.out.println("""firstFreeByteBeforeInsert  : Int = """ + $show(firstFreeByteBeforeInsert ));$skip(117); 
        val secondCopyBytes = firstFreeByteBeforeInsert - insertIndexBeforeSizeAdjustment - bytesForNextBeforeInsert;System.out.println("""secondCopyBytes  : Int = """ + $show(secondCopyBytes ));$skip(76); 
        val itemAfterItemDeltaIndexAfterInsert = insertIndex + bytesForItem;System.out.println("""itemAfterItemDeltaIndexAfterInsert  : Int = """ + $show(itemAfterItemDeltaIndexAfterInsert ));$skip(69); 
        writeUnsignedVarInt(numberOfIntsAfterInsert, targetArray, 0);$skip(65); 
        writeUnsignedVarInt(itemDelta, targetArray, insertIndex);$skip(97); 
        writeUnsignedVarInt(itemAfterItemDelta, targetArray, itemAfterItemDeltaIndexAfterInsert);$skip(96); 
        writeUnsignedVarIntBackwards(freeBytesAfterInsert, targetArray, targetArray.length - 1);$skip(186); 
        System.arraycopy(encoded, insertIndexBeforeSizeAdjustment + bytesForNextBeforeInsert, targetArray, itemAfterItemDeltaIndexAfterInsert + bytesForNextAfterInsert, secondCopyBytes);$skip(20); val res$0 = 
        targetArray;System.out.println("""res0: Array[Byte] = """ + $show(res$0));$skip(1110); 

  /**
   * Returns the index of the first byte of element
   * item, iff item is contained in the set.
   * Returns -1 otherwise.
   */
  def findIndex(item: Int): Int = {
    val intsTotal = size
    var intsTraversedSoFar = 0
    // Shift starting point by number of bytes spent on encoding the size.
    var i = bytesForVarint(size)
    var previousInt = -1
    var startingIndexOfCurrentInt = i
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
  };System.out.println("""findIndex: (item: Int)Int""");$skip(688); 

  def foreach(f: Int => Unit) {
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
  };System.out.println("""foreach: (f: Int => Unit)Unit""")}

}
