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

import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import scala.util.Random

object SplayIntSet {
  @inline final def nullNode = null.asInstanceOf[SplayNode]
  @inline final def ?(n: AnyRef) = n != null
}

final class SplayNode(
  var intSet: Any,
  var intervalFrom: Int = Int.MinValue,
  var intervalTo: Int = Int.MaxValue) {
  import SplayIntSet._
  var left: SplayNode = _
  var right: SplayNode = _

  def bytes: Int = {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          bs.length
        case ls: Array[Long] =>
          ls.length * 8
      }
    } else 0
  }

  override def toString = {
    val min = minElement
    val max = maxElement
    val density = ((size / range.toDouble) * 1000).round / 10.0
    s"SplayNode([$intervalFrom to $intervalTo], min = $min, max = $max, range = $range, #entries = $size, density = $density%, bytes = $bytes)"
  }

  @inline final def range: Long = {
    (intervalTo.toLong - intervalFrom) + 1
  }

  @inline final def density: Double = {
    size / range.toDouble
  }

  final def isEntireRangeContained: Boolean = {
    val r = range
    if (range > size.toLong) {
      false
    } else {
      size == range
    }
  }

  final def insert(i: Int, overheadFraction: Float): Boolean = {
    if (intSet != null) {
      val wasInserted = intSet match {
        // It's a FastInsertIntSet
        case bs: Array[Byte] =>
          val sizeBefore = new FastInsertIntSet(bs).size
          val after = new FastInsertIntSet(bs).insert(i, overheadFraction)
          intSet = after
          val sizeAfter = new FastInsertIntSet(after).size
          if (density > 0.12) {
            val bitSet = BitSet.create(intervalFrom, range.toInt)
            new FastInsertIntSet(after).foreach(new BitSet(bitSet).insert(_))
            intSet = bitSet
          }
          sizeAfter > sizeBefore
        case ls: Array[Long] =>
          new BitSet(ls).insert(i)
      }
      if (isEntireRangeContained) {
        // Int set being null means that all elements in the interval are contained.
        intSet = null
      }
      wasInserted
    } else {
      // Whole interval is contained already.
      false
    }
  }

  @inline final def isInRange(i: Int): Boolean = {
    i >= intervalFrom && i <= intervalTo
  }

  @inline final def contains(i: Int): Boolean = {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          new FastInsertIntSet(bs).contains(i)
        case ls: Array[Long] =>
          new BitSet(ls).contains(i)
      }
    } else {
      isInRange(i)
    }
  }

  /**
   * Assumes that the int set is not null.
   */
  @tailrec final def foreach(f: Int => Unit, pending: List[SplayNode] = Nil) {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          new FastInsertIntSet(bs).foreach(f)
        case ls: Array[Long] =>
          new BitSet(ls).foreach(f)
      }
    } else {
      // Int set being null means that all numbers in the interval are contained.
      var i = intervalFrom
      while (i <= intervalTo) {
        f(i)
        i += 1
      }
    }
    if (?(left) && ?(right)) {
      left.foreach(f, right :: pending)
    } else if (?(left)) {
      left.foreach(f, pending)
    } else if (?(right)) {
      right.foreach(f, pending)
    } else {
      pending match {
        case Nil =>
        case head :: tail =>
          head.foreach(f, tail)
      }
    }
  }

  final def validate {
    foreachNode {
      node =>
        if (?(node.left)) {
          assert(node.intervalFrom > node.left.intervalTo)
        }
        if (?(node.right)) {
          assert(node.intervalTo < node.right.intervalFrom)
        }
    }
  }

  @tailrec @inline final def foreachNode(f: SplayNode => Unit, pending: List[SplayNode] = Nil) {
    f(this)
    if (?(left) && ?(right)) {
      left.foreachNode(f, right :: pending)
    } else if (?(left)) {
      left.foreachNode(f, pending)
    } else if (?(right)) {
      right.foreachNode(f, pending)
    } else {
      pending match {
        case Nil =>
        case head :: tail =>
          head.foreachNode(f, tail)
      }
    }
  }

  @inline final def size: Int = {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          new FastInsertIntSet(bs).size
        case ls: Array[Long] =>
          new BitSet(ls).size
      }
    } else {
      intervalTo - intervalFrom + 1
    }
  }

  @inline final def minElement: Int = {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          new FastInsertIntSet(bs).min
        case ls: Array[Long] =>
          new BitSet(ls).min
      }
    } else {
      intervalFrom
    }
  }

  def maxElement: Int = {
    if (intSet != null) {
      intSet match {
        case bs: Array[Byte] =>
          new FastInsertIntSet(bs).max
        case ls: Array[Long] =>
          new BitSet(ls).max
      }
    } else {
      intervalTo
    }
  }

}

/**
 * Uses Splay trees to efficiently store integer sets.
 * Whilst an ordinary Splay tree contains one number per node, here each node
 * is responsible for a whole interval. The initial interval of the root node
 * spans all integers. Whenever a node reaches 'maxNodeIntSetSize', that node is split into
 * two nodes, and the interval for which the nodes are responsible is also split.
 */
abstract class SplayIntSet {
  import SplayIntSet._

  def printDiagnosticInfo {
    val id = Random.nextInt(10)
    println(s"$id: SplayIntSet diagnostic info:")
    if (root != null) {
      root.foreachNode(node => println(s"$id\t" + node.toString))
    }
  }

  // Approximate size in bytes of the internal representations. Does not consider object overhead.
  def bytes: Int = {
    if (root != null) {
      var bytes = 0
      root.foreachNode(node => bytes += node.bytes)
      bytes
    } else {
      0
    }
  }

  def overheadFraction: Float
  def maxNodeIntSetSize: Int

  var size: Int = 0
  var root: SplayNode = _

  // Avoid constructor to ensure that nothing unnecessary is stored.
  // The passed root cannot have any child nodes.
  def initializeWithRoot(r: SplayNode) {
    assert(r.left == null && r.right == null)
    root = r
    size = root.size
  }

  def toBuffer: Buffer[Int] = {
    val buffer = new ArrayBuffer[Int]
    if (size > 0) {
      root.foreach(buffer.append(_))
    }
    buffer
  }

  def toList: List[Int] = toBuffer.toList
  def toSet: Set[Int] = toBuffer.toSet

  /**
   * Asserts that the root has been set.
   */
  @inline final def foreach(f: Int => Unit) {
    if (size > 0) {
      root.foreach(f)
    }
  }

  /**
   * Returns true iff i is contained in the set.
   */
  def contains(i: Int): Boolean = {
    if (?(root)) {
      //root = splay(root, i)
      //root.contains(i)
      val node = find(root, i)
      node.contains(i)
    } else {
      false
    }
  }

  /**
   * Inserts i into the set, returns false if i was already contained.
   */
  def insert(i: Int): Boolean = {
    if (?(root)) {
      root = splay(root, i)
      val inserted = root.insert(i, overheadFraction)
      if (inserted) size += 1
      val nodeIntSet = root.intSet
      // Null would mean that the set is efficiently represented already.
      if (nodeIntSet != null &&
        nodeIntSet.isInstanceOf[Array[Byte]] &&
        new FastInsertIntSet(nodeIntSet.asInstanceOf[Array[Byte]]).size > maxNodeIntSetSize) {
        //println(s"Has now more than $maxNodeIntSetSize entires, splitting")
        val (set1, set2) = new FastInsertIntSet(nodeIntSet.asInstanceOf[Array[Byte]]).split(overheadFraction)
        val set2Min = new FastInsertIntSet(set2).min
        val newNode = new SplayNode(set1, root.intervalFrom, set2Min - 1)
        root.intSet = set2
        root.intervalFrom = set2Min
        insertNode(root, newNode)
      }
      return inserted
    } else {
      // Tree is empty.
      val repr = Ints.createEmptyFastInsertIntSet
      new FastInsertIntSet(repr).insert(i, overheadFraction)
      root = new SplayNode(repr)
      root.insert(i, overheadFraction)
      size += 1
      return true
    }
  }

  /**
   * Searches from root for an insertion point for 'newNode'.
   * There can be no other node in the tree that intersects with the interval of 'newNode'.
   */
  @tailrec private def insertNode(root: SplayNode, newNode: SplayNode) {
    if (newNode.intervalTo < root.intervalFrom) {
      val rootLeft = root.left
      if (?(rootLeft)) {
        insertNode(rootLeft, newNode)
      } else {
        root.left = newNode
      }
    } else if (newNode.intervalFrom > root.intervalTo) {
      val rootRight = root.right
      if (?(rootRight)) {
        insertNode(rootRight, newNode)
      } else {
        root.right = newNode
      }
    } else {
      throw new Exception(
        s"The new node interval from ${newNode.intervalFrom} to ${newNode.intervalTo} " +
          s"intersects with the interval ${root.intervalFrom} to ${root.intervalTo} of an existing node.")
    }
  }

  /**
   * Finds and returns the node that is responsible for the interval into
   * which i falls.
   */
  @tailrec private def find(node: SplayNode, i: Int): SplayNode = {
    if (node.intervalFrom > i) {
      find(node.left, i)
    } else if (node.intervalTo < i) {
      find(node.right, i)
    } else {
      node
    }
  }

  /**
   * Searches for the node that is responsible for key i, starting from node 'root'.
   * Splays the responsible node to the where 'root' is initially and returns it.
   */
  @tailrec private def splay(root: SplayNode, i: Int): SplayNode = {
    if (i < root.intervalFrom) {
      val rootLeft = root.left
      // We're going down to the left of the root.
      if (i < rootLeft.intervalFrom) {
        // We're going down left twice, rotate root.left.left up to be the new root, continue search from there.
        splay(rightRight(root, rootLeft, rootLeft.left), i)
      } else if (i > rootLeft.intervalTo) {
        // We're going down left and then right, rotate root.left.right up to be the new root, continue search from there.
        splay(leftRight(root, rootLeft, rootLeft.right), i)
      } else {
        // root.left is the new root, rotate it up.
        right(root, rootLeft)
      }
    } else if (i > root.intervalTo) {
      val rootRight = root.right
      // We're going down to the right of the root.
      if (i < rootRight.intervalFrom) {
        // We're going down right and then left, rotate root.right.left up to be the new root, continue search from there.
        splay(rightLeft(root, rootRight, rootRight.left), i)
      } else if (i > rootRight.intervalTo) {
        // We're going down right and then left, rotate root.right.right up to be the new root, continue search from there.
        splay(leftLeft(root, rootRight, rootRight.right), i)
      } else {
        // root.right is the new root, rotate it up.
        left(root, rootRight)
      }
    } else {
      // i falls into the interval of the root already. We're done.
      root
    }
  }

  def leftRight(root: SplayNode, rootLeft: SplayNode, rootLeftRight: SplayNode): SplayNode = {
    right(root, left(rootLeft, rootLeftRight))
  }

  def rightLeft(root: SplayNode, rootRight: SplayNode, rootRightLeft: SplayNode): SplayNode = {
    left(root, right(rootRight, rootRightLeft))
  }

  def rightRight(root: SplayNode, rootLeft: SplayNode, rootLeftLeft: SplayNode): SplayNode = {
    right(root, right(rootLeft, rootLeftLeft))
  }

  def leftLeft(root: SplayNode, rootRight: SplayNode, rootRightRight: SplayNode): SplayNode = {
    left(root, left(rootRight, rootRightRight))
  }

  /**
   * Rotates 'rootRight' left in order to make it the new root, returns that new root.
   */
  def left(root: SplayNode, rootRight: SplayNode): SplayNode = {
    root.right = rootRight.left
    rootRight.left = root
    rootRight
  }

  /**
   * Rotates 'rootLeft' right in order to make it the new root, returns that new root.
   */
  def right(root: SplayNode, rootLeft: SplayNode): SplayNode = {
    root.left = rootLeft.right
    rootLeft.right = root
    rootLeft
  }

}
