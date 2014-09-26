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

/*
 * HashMap Int keys and Double values.
 */
class IntDoubleHashMap(
  initialSize: Int = 32768,
  rehashFraction: Float = 0.5f) {
  assert(initialSize > 0)
  final var maxSize = nextPowerOfTwo(initialSize)
  assert(1.0f >= rehashFraction && rehashFraction > 0.1f, "Unreasonable rehash fraction.")
  assert(maxSize > 0 && maxSize >= initialSize, "Initial size is too large.")
  private[this] final var maxElements: Int = (rehashFraction * maxSize).floor.toInt
  private[this] final var values = new Array[Double](maxSize)
  private[this] final var keys = new Array[Int](maxSize) // 0 means empty
  private[this] final var mask = maxSize - 1

  final def size: Int = numberOfElements
  final def isEmpty: Boolean = numberOfElements == 0
  private[this] final var numberOfElements = 0

  final def clear {
    keys = new Array[Int](maxSize)
    numberOfElements = 0
  }

  def toScalaMap: Map[Int, Double] = {
    keys.zip(values).filter(_._1 != 0).toMap
  }

  private[this] final def tryDouble {
    // 1073741824 is the largest size and cannot be doubled anymore.
    if (maxSize != 1073741824) {
      val oldValues = values
      val oldKeys = keys
      val oldNumberOfElements = numberOfElements
      maxSize *= 2
      maxElements = (rehashFraction * maxSize).floor.toInt
      values = new Array[Double](maxSize)
      keys = new Array[Int](maxSize)
      mask = maxSize - 1
      numberOfElements = 0
      var i = 0
      var elementsMoved = 0
      while (elementsMoved < oldNumberOfElements) {
        val oldKey = oldKeys(i)
        if (oldKey != 0) {
          put(oldKey, oldValues(i))
          elementsMoved += 1
        }
        i += 1
      }
    }
  }

  @inline final def foreach(f: (Int, Double) => Unit) {
    var i = 0
    var elementsProcessed = 0
    while (elementsProcessed < numberOfElements) {
      val key = keys(i)
      if (key != 0) {
        val value = values(i)
        f(key, value)
        elementsProcessed += 1
      }
      i += 1
    }
  }

  /**
   * Like foreach, but removes the entry after applying the function.
   */
  @inline final def process(f: (Int, Double) => Unit) {
    var i = 0
    var elementsProcessed = 0
    while (elementsProcessed < numberOfElements) {
      val key = keys(i)
      if (key != 0) {
        val value = values(i)
        f(key, value)
        elementsProcessed += 1
        keys(i) = 0
      }
      i += 1
    }
    numberOfElements = 0
  }

  final def remove(key: Int) {
    remove(key, true)
  }

  private final def remove(key: Int, optimize: Boolean) {
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && (key != keyAtPosition)) {
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
        val value = values(currentPosition)
        removeCurrentEntry
        put(keyAtPosition, value)
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
  
  final def apply(key: Int) = get(key)

  @inline final def get(key: Int): Double = {
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    if (keyAtPosition != 0) {
      values(position)
    } else {
      0
    }
  }

  final def update(key: Int, value: Double) {
    put(key, value)
  }

  /**
   * Adds 'delta' to the value for key 'key'. If that key had not value so far, assumes that its value was 0.
   */
  final def addToValueForKey(key: Int, delta: Double) {
    assert(key != 0, "Key cannot be 0")
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    val hadExistingEntryForKey = keyAtPosition == key
    if (!hadExistingEntryForKey) {
      keys(position) = key
      values(position) = delta
      numberOfElements += 1
      if (numberOfElements >= maxElements) {
        tryDouble
        if (numberOfElements >= maxSize) {
          throw new OutOfMemoryError("The hash map is full and cannot be expanded any further.")
        }
      }
    } else {
      values(position) = values(position) + delta
    }
  }

  /**
   * Key 0 is not allowed!
   * Returns if an existing entry was overridden.
   */
  def put(key: Int, value: Double): Boolean = {
    assert(key != 0, "Key cannot be 0")
    var position = keyToPosition(key)
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    val hadExistingEntryForKey = keyAtPosition == key
    if (!hadExistingEntryForKey) {
      keys(position) = key
      values(position) = value
      numberOfElements += 1
      if (numberOfElements >= maxElements) {
        tryDouble
        if (numberOfElements >= maxSize) {
          throw new OutOfMemoryError("The hash map is full and cannot be expanded any further.")
        }
      }
    } else {
      values(position) = value
    }
    hadExistingEntryForKey
  }

  private[this] final def keyToPosition(key: Int) = {
    key & mask
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
