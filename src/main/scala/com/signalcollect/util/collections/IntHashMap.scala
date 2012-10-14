/*
 *  @author Philip Stutz
 *
 *  Copyright 2012 University of Zurich
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

import reflect.{ ClassTag, classTag }
import scala.util.MurmurHash

class IntHashMap[Value <: AnyRef: ClassTag](
    initialSize: Int = 32768,
    rehashFraction: Float = 0.85f) extends Traversable[Value] {
  assert(initialSize > 0)
  private[this] final var maxSize = nextPowerOfTwo(initialSize)
  assert(maxSize > 0 && maxSize >= initialSize, "Initial size is too large.")
  private[this] final var maxElements: Int = (rehashFraction * maxSize).floor.toInt
  private[this] final var values = classTag[Value].newArray(maxSize) // 0 means empty
  private[this] final var keys = new Array[Int](maxSize)
  private[this] final var mask = maxSize - 1
  private[this] final var nextPositionToProcess = 0

  final override def size = numberOfElements
  final override def isEmpty = numberOfElements == 0
  private[this] final var numberOfElements = 0

  final def clear {
    values = classTag[Value].newArray(maxSize)
    keys = new Array[Int](maxSize)
    numberOfElements = 0
    nextPositionToProcess = 0
  }

  final def foreach[U](f: Value => U) {
    var i = 0
    var elementsProcessed = 0
    while (elementsProcessed < numberOfElements) {
      val vertex = values(i)
      if (vertex != null) {
        f(vertex)
        elementsProcessed += 1
      }
      i += 1
    }
  }

  // Removes the vertices after they have been processed.
  final def process[U](p: Value => U, numberOfVertices: Option[Int] = None) {
    val limit = math.min(numberOfElements, numberOfVertices.getOrElse(numberOfElements))
    var elementsProcessed = 0
    while (elementsProcessed < limit) {
      val vertex = values(nextPositionToProcess)
      if (vertex != null) {
        p(vertex)
        elementsProcessed += 1
        // Don't optimize, most of the next entries get removed anyway.
        keys(nextPositionToProcess) = 0
        values(nextPositionToProcess) = null.asInstanceOf[Value]
        numberOfElements -= 1
      }
      nextPositionToProcess = (nextPositionToProcess + 1) & mask
    }
  }

  private[this] final def tryDouble {
    // 1073741824 is the largest size and cannot be doubled anymore.
    if (maxSize != 1073741824) {
      val oldSize = maxSize
      val oldValues = values
      val oldKeys = keys
      val oldNumberOfElements = numberOfElements
      maxSize *= 2
      maxElements = (rehashFraction * maxSize).floor.toInt
      values = classTag[Value].newArray(maxSize)
      keys = new Array[Int](maxSize)
      mask = maxSize - 1
      numberOfElements = 0
      var i = 0
      var elementsMoved = 0
      while (elementsMoved < oldNumberOfElements) {
        if (oldKeys(i) != 0) {
          putWithEntryKey(oldKeys(i), oldValues(i))
          elementsMoved += 1
        }
        i += 1
      }
    }
  }

  final def remove(key: Int) {
    remove(key, true)
  }

  private[this] final def remove(key: Int, optimize: Boolean) {
    removeWithEntryKey(actualKeyToEntryKey(key), optimize)
  }

  private[this] final def removeWithEntryKey(entryKey: Int, optimize: Boolean) {
    var position = entryKeyToPosition(entryKey)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && entryKey != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    // We can only remove the entry if it was found.
    if (keyAtPosition != 0) {
      keys(position) = 0
      values(position) = null.asInstanceOf[Value]
      numberOfElements -= 1
      if (optimize) {
        // Try to reinsert all elements that are not optimally placed until an empty position is found.
        // See http://stackoverflow.com/questions/279539/best-way-to-remove-an-entry-from-a-hash-table
        position = ((position + 1) & mask)
        keyAtPosition = keys(position)
        while (keyAtPosition != 0) {
          if (entryKeyToPosition(keyAtPosition) != position) {
            val value = values(position)
            keys(position) = 0
            values(position) = null.asInstanceOf[Value]
            numberOfElements -= 1
            putWithEntryKey(keyAtPosition, value)
          }
          position = ((position + 1) & mask)
          keyAtPosition = keys(position)
        }
      }
    }
  }

  final def get(key: Int): Value = {
    val entryKey = actualKeyToEntryKey(key)
    var position = entryKeyToPosition(entryKey)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && entryKey != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    if (keyAtPosition != 0) {
      values(position)
    } else {
      null.asInstanceOf[Value]
    }
  }

  private[this] final def entryKeyToPosition(entryKey: Int) = {
    entryKey.hashCode & mask
  }

  //assert(entryKey != 0)
  private[this] final def entryKeyToActualKey(entryKey: Int) = {
    if (entryKey == Int.MinValue) 0 else entryKey
  }

  //assert(actualKey != Int.MinValue)
  private[this] final def actualKeyToEntryKey(actualKey: Int) = {
    if (actualKey == 0) Int.MinValue else actualKey
  }

  // assert(key != Int.MinValue)
  // Only correct if a positive Int is used as the key.
  // Only put if no vertex with the same id is present. If a vertex was put, return true.
  final def put(key: Int, value: Value): Boolean = {
    putWithEntryKey(actualKeyToEntryKey(key), value)
  }

  private[this] final def putWithEntryKey(entryKey: Int, value: Value): Boolean = {
    var position = entryKeyToPosition(entryKey)
    var entryKeyAtPosition = keys(position)
    while (entryKeyAtPosition != 0 && entryKey != entryKeyAtPosition) {
      position = (position + 1) & mask
      entryKeyAtPosition = keys(position)
    }
    var doPut = entryKeyAtPosition == 0
    if (doPut) {
      keys(position) = entryKey
      values(position) = value
      numberOfElements += 1
      if (numberOfElements >= maxElements) {
        if (numberOfElements == maxSize) {
          throw new OutOfMemoryError("The hash map is full and cannot be expanded any further.")
        }
        tryDouble
      }
    }
    doPut
  }

  // assert(x > 0)
  private[this] final def nextPowerOfTwo(x: Int): Int = {
    var r = x - 1
    r |= r >> 1
    r |= r >> 2
    r |= r >> 4
    r |= r >> 8
    r |= r >> 16
    r + 1
  }

}