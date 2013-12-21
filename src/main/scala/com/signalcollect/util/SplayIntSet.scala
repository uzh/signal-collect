package com.signalcollect.util

import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec

object SplayIntSet {
  @inline def nullNode = null.asInstanceOf[SplayNode]
  @inline def ?(n: AnyRef) = n != null
}

class SplayNode {
  import SplayIntSet._
  var intSet: Array[Byte] = Ints.createEmptyFastInsertIntSet
  var left: SplayNode = _
  var right: SplayNode = _

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
    // TODO: Parallelize using async if we can make the BulkMessageBus thread safe.
    // Recursive impl: danger of stack overflow.
    //    new FastInsertIntSet(intSet).foreach(f)
    //    if (?(left)) left.foreach(f)
    //    if (?(right)) right.foreach(f)
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

  def min: Int = new FastInsertIntSet(intSet).min

}

abstract class SplayIntSet {
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
      root = splay(root, i, false)
      val inserted = root.insert(i, overheadFraction)
      if (inserted) size += 1
      val nodeIntSet = new FastInsertIntSet(root.intSet)
      val nodeIntSetSize = nodeIntSet.size
      if (nodeIntSetSize > maxNodeIntSetSize) {
        val (set1, set2) = new FastInsertIntSet(root.intSet).split(overheadFraction)
        root.intSet = set1
        root = splay(root, new FastInsertIntSet(set2).min, true)
        root.intSet = set2
      }
      return inserted
    } else {
      // Tree is empty.
      root = new SplayNode
      root.insert(i, overheadFraction)
      size += 1
      return true
    }
  }

  /**
   * Searches for the node that is responsible for key i, starting from node 'root'.
   * Splays the responsible node to the where 'root' is initially and returns it.
   * If create is set, then a new node for that key is created and splayed up to the root,
   * but only if it is not already contained.
   */
  @tailrec private def splay(root: SplayNode, i: Int, create: Boolean): SplayNode = {
    val rootMin = root.min
    if (i < rootMin) {
      val rootLeft = root.left
      if (?(rootLeft)) {
        val rootLeftMin = rootLeft.min
        if (i < rootLeftMin) {
          val rootLeftLeft = rootLeft.left
          if (?(rootLeftLeft)) {
            splay(rightRight(root, rootLeft, rootLeftLeft), i, create)
          } else {
            if (create) {
              val newNode = new SplayNode
              rootLeft.left = newNode
              rightRight(root, rootLeft, newNode)
            } else {
              right(root, rootLeft)
            }
          }
        } else if (i > rootLeftMin) {
          val rootLeftRight = rootLeft.right
          if (?(rootLeftRight)) {
            splay(leftRight(root, rootLeft, rootLeftRight), i, create)
          } else {
            if (create) {
              val newNode = new SplayNode
              rootLeft.right = newNode
              leftRight(root, rootLeft, newNode)
            } else {
              // no creation necessary.
              right(root, rootLeft)
            }
          }
        } else {
          // i == root.left.min, no creation necessary.
          right(root, rootLeft)
        }
      } else {
        // rootLeft == null
        if (create) {
          val newNode = new SplayNode
          root.left = newNode
          right(root, newNode)
        } else {
          root
        }
      }
    } else if (i > rootMin) {
      val rootRight = root.right
      if (?(rootRight)) {
        val rootRightMin = rootRight.min
        if (i < rootRightMin) {
          val rootRightLeft = rootRight.left
          if (?(rootRightLeft)) {
            splay(rightLeft(root, rootRight, rootRightLeft), i, create)
          } else {
            if (create) {
              val newNode = new SplayNode
              rootRight.left = newNode
              rightLeft(root, rootRight, newNode)
            } else {
              left(root, rootRight)
            }
          }
        } else if (i > rootRightMin) {
          val rootRightRight = rootRight.right
          if (?(rootRightRight)) {
            splay(leftLeft(root, rootRight, rootRightRight), i, create)
          } else {
            if (create) {
              val newNode = new SplayNode
              rootRight.right = newNode
              leftLeft(root, rootRight, newNode)
            } else {
              // no creation necessary.
              left(root, rootRight)
            }
          }
        } else {
          // i == root.right.min, no creation necessary.
          left(root, rootRight)
        }
      } else {
        // rootRight == null
        if (create) {
          val newNode = new SplayNode
          root.right = newNode
          left(root, newNode)
        } else {
          root
        }
      }
    } else {
      // i == root.min, no creation necessary.
      root
    }
  }

  def leftRight(root: SplayNode, rootLeft: SplayNode, rootLeftRight: SplayNode): SplayNode = {
    val splayed = left(rootLeft, rootLeftRight)
    root.left = splayed
    right(splayed, root)
  }

  def rightLeft(root: SplayNode, rootRight: SplayNode, rootRightLeft: SplayNode): SplayNode = {
    val splayed = right(rootRight, rootRightLeft)
    root.right = splayed
    left(root, splayed)
  }

  def rightRight(root: SplayNode, rootLeft: SplayNode, rootLeftLeft: SplayNode): SplayNode = {
    right(right(root, rootLeft), rootLeftLeft)
  }

  def leftLeft(root: SplayNode, rootRight: SplayNode, rootRightRight: SplayNode): SplayNode = {
    left(left(root, rootRight), rootRightRight)
  }

  def left(root: SplayNode, rootRight: SplayNode): SplayNode = {
    root.right = rootRight.left
    rootRight.left = root
    rootRight
  }

  def right(root: SplayNode, rootLeft: SplayNode): SplayNode = {
    root.left = rootLeft.right
    rootLeft.right = root
    rootLeft
  }

}
