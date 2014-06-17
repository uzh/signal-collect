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

object SplayIntSet {
  @inline def nullNode = null.asInstanceOf[SplayNode]
  @inline def ?(n: AnyRef) = n != null
}

final class SplayNode(
  var intSet: Array[Byte],
  var intervalFrom: Int = Int.MinValue,
  var intervalTo: Int = Int.MaxValue) {
  import SplayIntSet._
  var left: SplayNode = _
  var right: SplayNode = _

  override def toString = {
    s"SplayNode([$intervalFrom to $intervalTo], smallest element = $minElement, #entries = $size)"
  }

  def insert(i: Int, overheadFraction: Float): Boolean = {
    val sizeBefore = new FastInsertIntSet(intSet).size
    intSet = new FastInsertIntSet(intSet).insert(i, overheadFraction)
    val sizeAfter = new FastInsertIntSet(intSet).size
    sizeAfter > sizeBefore
  }

  /**
   * Assumes that the int set is not null.
   */
  @tailrec final def foreach(f: Int => Unit, pending: List[SplayNode] = Nil) {
    new FastInsertIntSet(intSet).foreach(f)
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

  @tailrec final def foreachNode(f: SplayNode => Unit, pending: List[SplayNode] = Nil) {
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

  def size: Int = new FastInsertIntSet(intSet).size

  def minElement: Int = new FastInsertIntSet(intSet).min

}

/**
 * Uses Splay trees to efficiently store integer sets.
 * Whilst an ordinary Splay tree contains one number per node, here each node
 * is responsible for a whole interval. The initial interval of the root node
 * spans all integers. Whenever a node reaches 'maxNodeIntSetSize', that node is split into
 * two nodes, and the interval for which the nodes are responsible is also split.
 */
abstract class SplayIntSet extends Traversable[Int] {
  import SplayIntSet._

  def overheadFraction: Float
  def maxNodeIntSetSize: Int

  var size: Int = 0
  var root: SplayNode = _

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
  @inline def foreach(f: Int => Unit) {
    if (size > 0) {
      root.foreach(f)
    }
  }

  /**
   * Inserts i into the set, returns false if i was already contained.
   */
  def insert(i: Int): Boolean = {
    if (?(root)) {
      root = splay(root, i)
      val inserted = root.insert(i, overheadFraction)
      //println(s"Inserted $i into ${new FastInsertIntSet(root.intSet).toList}")
      if (inserted) size += 1
      val nodeIntSet = new FastInsertIntSet(root.intSet)
      val nodeIntSetSize = nodeIntSet.size
      if (nodeIntSetSize > maxNodeIntSetSize) {
        //println(s"Has now more than $maxNodeIntSetSize entires, splitting")
        val (set1, set2) = new FastInsertIntSet(root.intSet).split(overheadFraction)
        val set2Min = new FastInsertIntSet(set2).min
        val newNode = new SplayNode(set1, root.intervalFrom, set2Min - 1)
        root.intSet = set2
        root.intervalFrom = set2Min
        //        println(s"Root: $root")
        //        println(s"New node: $newNode")
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
