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

import scala.Array.canBuildFrom
import scala.reflect.ClassTag

class IntHashSet(
  initialSize: Int = 32768,
  rehashFraction: Float = 0.75f) {
  assert(initialSize > 0)
  final var maxSize = nextPowerOfTwo(initialSize)
  assert(1.0f >= rehashFraction && rehashFraction > 0.1f, "Unreasonable rehash fraction.")
  assert(maxSize > 0 && maxSize >= initialSize, "Initial size is too large.")
  private[this] final var maxElements: Int = (rehashFraction * maxSize).floor.toInt
  private[this] final var keys = new Array[Int](maxSize) // 0 means empty
  private[this] final var mask = maxSize - 1
  private[this] final var nextPositionToProcess = 0

  final def size: Int = numberOfElements
  final def isEmpty: Boolean = numberOfElements == 0
  private[this] final var numberOfElements = 0

  final def clear {
    keys = new Array[Int](maxSize)
    numberOfElements = 0
    nextPositionToProcess = 0
  }

  def toScalaSet: Set[Int] = {
    keys.filter(_ != 0).toSet
  }

  private[this] final def tryDouble {
    // 1073741824 is the largest size and cannot be doubled anymore.
    if (maxSize != 1073741824) {
      val oldKeys = keys
      val oldNumberOfElements = numberOfElements
      maxSize *= 2
      maxElements = (rehashFraction * maxSize).floor.toInt
      keys = new Array[Int](maxSize)
      mask = maxSize - 1
      numberOfElements = 0
      var i = 0
      var elementsMoved = 0
      while (elementsMoved < oldNumberOfElements) {
        val oldKey = oldKeys(i)
        if (oldKey != 0) {
          add(oldKey)
          elementsMoved += 1
        }
        i += 1
      }
    }
  }

  final def foreach(f: Int => Unit) {
    var i = 0
    var elementsProcessed = 0
    while (elementsProcessed < numberOfElements) {
      val key = keys(i)
      if (key != 0) {
        f(key)
        elementsProcessed += 1
      }
      i += 1
    }
  }

  final def remove(key: Int) {
    remove(key, true)
  }

  private final def remove(key: Int, optimize: Boolean) {
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    // We can only remove the entry if it was found.
    if (keyAtPosition != 0) {
      keys(position) = 0
      numberOfElements -= 1
      if (optimize) {
        optimizeFromPosition((position + 1) & mask)
      }
    }
  }

  // Try to reinsert all elements that are not optimally placed until an empty position is found.
  // See http://stackoverflow.com/questions/279539/best-way-to-remove-an-entry-from-a-hash-table
  private[this] final def optimizeFromPosition(startingPosition: Int) {
    var currentPosition = startingPosition
    var keyAtPosition = keys(currentPosition)
    while (isCurrentPositionOccupied) {
      val perfectPositionForEntry = keyToPosition(keyAtPosition)
      if (perfectPositionForEntry != currentPosition) {
        // We try to optimize the placement of the entry by removing and then reinserting it.
        removeCurrentEntry
        add(keyAtPosition)
      }
      advance
    }
    @inline def advance {
      currentPosition = ((currentPosition + 1) & mask)
      keyAtPosition = keys(currentPosition)
    }
    @inline def isCurrentPositionOccupied = {
      keyAtPosition != 0
    }
    @inline def removeCurrentEntry {
      keys(currentPosition) = 0
      numberOfElements -= 1
    }
  }

  final def apply(key: Int) = contains(key)

  @inline final def contains(key: Int): Boolean = {
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    if (keyAtPosition != 0) {
      true
    } else {
      false
    }
  }

  /**
   * Returns true iff this entry already existed.
   */
  def add(key: Int): Boolean = {
    assert(key != 0, "Key cannot be 0")
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    val alreadyContained = keyAtPosition == key
    if (!alreadyContained) {
      keys(position) = key
      numberOfElements += 1
      if (numberOfElements >= maxElements) {
        tryDouble
        if (numberOfElements >= maxSize) {
          throw new OutOfMemoryError("The hash map is full and cannot be expanded any further.")
        }
      }
    }
    alreadyContained
  }

  private[this] final def keyToPosition(key: Int) = {
    key.hashCode & mask
  }

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
