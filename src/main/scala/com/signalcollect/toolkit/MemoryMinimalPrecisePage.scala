/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
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
 *  
 */

package com.signalcollect.toolkit

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex

// Only supports unsigned integers.
object CompactIntSet {
  def create(ints: Array[Int]): Array[Byte] = {
    val sorted = ints.sorted // TODO: Sort in place.
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

  def foreach(encoded: Array[Byte], f: Int => Unit) {
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

  private val hasAnotherByte = Integer.parseInt("10000000", 2)
  private val leastSignificant7BitsMask = Integer.parseInt("01111111", 2)
  private val everythingButLeastSignificant7Bits = ~leastSignificant7BitsMask

  // Same as https://developers.google.com/protocol-buffers/docs/encoding
  private def writeUnsignedVarInt(i: Int, out: DataOutputStream) {
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

}

final class MemoryMinimalPrecisePage(val id: Int) extends Vertex[Int, Double, Int, Double] {

  var state = 0.15
  var pendingToSignal: Double = 0.15
  var outEdges = 0

  def targetIds: Traversable[Int] = {
    new Traversable[Int] {
      def foreach[U](f: Int => U) {
        if (outEdges != 0) {
          CompactIntSet.foreach(targetIdArray, f(_))
        }
      }
    }
  }

  def setState(s: Double) {
    state = s
  }

  protected var targetIdArray: Array[Byte] = null

  override def addEdge(e: Edge[Int], graphEditor: GraphEditor[Int, Double]): Boolean = {
    throw new UnsupportedOperationException
  }

  def setTargetIdArray(links: Array[Int]) = {
    outEdges = links.length
    targetIdArray = CompactIntSet.create(links)
  }

  override def deliverSignalWithSourceId(signal: Double, sourceId: Int, ge: GraphEditor[Int, Double]): Boolean = {
    throw new Exception("This PageRank algorithm should never receive a source ID.")
  }

  override def deliverSignalWithoutSourceId(signal: Double, ge: GraphEditor[Int, Double]): Boolean = {
    val dampedDelta = 0.85 * signal
    state += dampedDelta
    pendingToSignal += dampedDelta
    true
  }

  override def executeSignalOperation(graphEditor: GraphEditor[Int, Double]) {
    val tIds = targetIdArray
    val tIdLength = outEdges
    val signal = pendingToSignal / tIdLength
    CompactIntSet.foreach(targetIdArray, graphEditor.sendSignal(signal, _))
    pendingToSignal = 0
  }

  def executeCollectOperation(graphEditor: GraphEditor[Int, Double]) {
    throw new UnsupportedOperationException
  }

  override def scoreSignal: Double = {
    if (outEdges > 0) pendingToSignal else 0
  }

  def scoreCollect = 0 // because signals are directly collected at arrival

  def edgeCount = outEdges

  def afterInitialization(graphEditor: GraphEditor[Int, Double]) = {}
  def beforeRemoval(graphEditor: GraphEditor[Int, Double]) = {}

  override def removeEdge(targetId: Int, graphEditor: GraphEditor[Int, Double]): Boolean = {
    throw new UnsupportedOperationException
  }

  override def removeAllEdges(graphEditor: GraphEditor[Int, Double]): Int = {
    throw new UnsupportedOperationException
  }

  override def toString = "MemoryMinimalPrecisePage(" + id + ", " + state + ")"
}
