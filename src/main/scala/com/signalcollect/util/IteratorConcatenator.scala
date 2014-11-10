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
 *
 */

package com.signalcollect.util

import scala.annotation.tailrec
import scala.collection.mutable.Queue

/**
 * To avoid https://issues.scala-lang.org/browse/SI-8428, which is not really fixed.
 * 
 * Unfortunately I could not reproduce the problem outside of a large and complex TripleRush evaluation.
 */
final class IteratorConcatenator[U] extends Iterator[U] {

  val iterators = new Queue[Iterator[U]]()

  def clear {
    iterators.clear
  }

  def appendIterator(i: Iterator[U]) {
    iterators.enqueue(i)
  }

  def next: U = {
    iterators.head.next
  }

  @tailrec def hasNext: Boolean = {
    if (iterators.isEmpty) {
      false
    } else {
      val headHasNext = iterators.head.hasNext
      if (!headHasNext) {
        iterators.dequeue
        hasNext
      } else {
        true
      }
    }
  }
}
