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

package com.signalcollect.storage

import com.signalcollect.interfaces.VertexStore
import com.signalcollect.Vertex
import scala.util.MurmurHash
import scala.annotation.tailrec

class VertexMap(
    initialSize: Int = 32768,
    rehashFraction: Float = 0.75f) extends VertexStore {
  assert(initialSize > 0)
  private[this] final var maxSize = nextPowerOfTwo(initialSize)
  assert(maxSize > 0 && maxSize >= initialSize, "Initial size is too large.")
  private[this] final var maxElements = rehashFraction * maxSize
  private[this] final var values = new Array[Vertex[_, _]](maxSize)
  private[this] final var keys = new Array[Int](maxSize)
  private[this] final var mask = maxSize - 1
  private[this] final var nextPositionToProcess = 0

  final override def size = numberOfElements
  final def isEmpty = numberOfElements == 0
  private[this] final var numberOfElements = 0

  final def clear {
    maxElements = rehashFraction * maxSize
    values = new Array[Vertex[_, _]](maxSize)
    keys = new Array[Int](maxSize)
    mask = maxSize - 1
    numberOfElements = 0
  }

  final def foreach[U](f: Vertex[_, _] => U) {
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
  final def process[U](p: Vertex[_, _] => U, numberOfVertices: Option[Int] = None) {
    val limit = math.min(numberOfElements, numberOfVertices.getOrElse(numberOfElements))
    var elementsProcessed = 0
    while (elementsProcessed < limit) {
      val vertex = values(nextPositionToProcess)
      if (vertex != null) {
        p(vertex)
        elementsProcessed += 1
        // Don't optimize, most of the next entries get removed anyway.
        keys(nextPositionToProcess) = 0
        values(nextPositionToProcess) = null
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
      maxSize *= 2
      maxElements = rehashFraction * maxSize
      values = new Array[Vertex[_, _]](maxSize)
      keys = new Array[Int](maxSize)
      mask = maxSize - 1
      numberOfElements = 0
      var i = 0
      while (i < oldSize) {
        if (oldKeys(i) != 0) {
          put(oldValues(i))
        }
        i += 1
      }
    }
  }

  final def remove(id: Any) = remove(id, true)

  private final def remove(id: Any, optimize: Boolean) {
    val key = MurmurHash.finalizeHash(id.hashCode) | Int.MinValue
    var position = key & mask
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition && id != values(position).id) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    // We can only remove the entry if it was found.
    if (keyAtPosition != 0) {
      keys(position) = 0
      values(position) = null
      numberOfElements -= 1
      if (optimize) {
        // Try to reinsert all elements that are not optimally placed until an empty position is found.
        // See http://stackoverflow.com/questions/279539/best-way-to-remove-an-entry-from-a-hash-table
        position = ((position + 1) & mask)
        keyAtPosition = keys(position)
        while (keyAtPosition != 0) {
          if ((keyAtPosition & mask) != position) {
            val vertex = values(position)
            remove(vertex.id, false)
            put(vertex)
          }
          position = ((position + 1) & mask)
          keyAtPosition = keys(position)
        }
      }
    }
  }

  final def get(vertexId: Any): Vertex[_, _] = {
    val key = MurmurHash.finalizeHash(vertexId.hashCode) | Int.MinValue
    var position = key & mask
    var keyAtPosition = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition && vertexId != values(position).id) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    if (keyAtPosition != 0) {
      values(position)
    } else {
      null.asInstanceOf[Vertex[_, _]]
    }
  }

  // Only put if no vertex with the same id is present. If a vertex was put, return true.
  final def put(vertex: Vertex[_, _]): Boolean = {
    val key = MurmurHash.finalizeHash(vertex.id.hashCode) | Int.MinValue
    var position = key & mask
    var keyAtPosition: Int = keys(position)
    while (keyAtPosition != 0 && key != keyAtPosition && vertex.id != values(position).id) {
      position = (position + 1) & mask
      keyAtPosition = keys(position)
    }
    var doPut = keyAtPosition == 0
    if (doPut) {
      keys(position) = key
      values(position) = vertex
      numberOfElements += 1
      if (numberOfElements >= maxElements) {
        if (numberOfElements >= maxSize) {
          throw new OutOfMemoryError("The hash map is full and cannot be expanded any further.")
        }
        tryDouble
      }
    }
    doPut
  }

  private[this] final def nextPowerOfTwo(x: Int): Int = {
    assert(x > 0)
    var r = x - 1
    r |= r >> 1
    r |= r >> 2
    r |= r >> 4
    r |= r >> 8
    r |= r >> 16
    r + 1
  }

}