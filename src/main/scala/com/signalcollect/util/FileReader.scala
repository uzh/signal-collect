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

import java.io.FileInputStream
import java.io.BufferedInputStream
import scala.annotation.switch

object FileReader {

  /**
   * Returns an iterator for all Ints in the file at the file path.
   * @Note The numbers need to be positive, fit into a Java Integer,
   * be encoded in ASCII/UTF8 format and can only be separated by
   * single spaces, single newlines, and tabs.
   * Whole lines can be commented out by starting them with '#'.
   *
   * @note Negative numbers are unsupported.
   */
  @inline def intIterator(filePath: String): Iterator[Int] = {
    new AsciiIntIterator(filePath)
  }

  /**
   * Sequentially applies the function 'p' to all integers that
   * are read from file 'filePath'.
   * @Note The numbers need to be positive, fit into a Java Integer,
   * be encoded in ASCII/UTF8 format and can only be separated by
   * single spaces, single newlines, and tabs.
   * Whole lines can be commented out by starting them with '#'.
   *
   * @note Negative numbers are unsupported.
   */
  @inline def processInts(filePath: String, p: Int => Unit) {
    val BLOCK_SIZE = 8 * 32768
    val in = new BufferedInputStream(new FileInputStream(filePath))
    var currentNumber = 0
    var inputIndex = 0
    val buf = new Array[Byte](BLOCK_SIZE)
    var read = in.read(buf)
    var commentedOut = false
    var numberStarted = false
    while (read != -1) {
      while (inputIndex < read) {
        val b = buf(inputIndex)
        inputIndex += 1
        if (!commentedOut) {
          (b: @switch) match {
            case '#' =>
              commentedOut = true
              if (numberStarted) {
                p(currentNumber)
                numberStarted = false
                currentNumber = 0
              }
            case ' ' | '\n' | '\t' =>
              if (numberStarted) {
                p(currentNumber)
                numberStarted = false
                currentNumber = 0
              }
            case other =>
              numberStarted = true
              currentNumber = 10 * currentNumber + b - '0'
          }
        } else if (b == '\n') {
          commentedOut = false
        }
      }
      inputIndex -= read
      read = in.read(buf)
    }
    in.close
  }

}

private final class AsciiIntIterator(filePath: String) extends Iterator[Int] {
  var initialized = false
  final val BLOCK_SIZE = 8 * 32768
  var in: BufferedInputStream = _
  var commentedOut = false
  var numberStarted = false
  var currentNumber = -1
  var inputIndex = 0
  var read: Int = _
  var buf: Array[Byte] = _

  @inline def next: Int = {
    if (currentNumber == -1) {
      val legalCall = hasNext
      if (legalCall) {
        val c = currentNumber
        currentNumber = -1
        c
      } else {
        throw new Exception("Next was called when there was no next element.")
      }
    } else {
      val c = currentNumber
      currentNumber = -1
      c
    }
  }

  @inline def hasNext: Boolean = {
    if (currentNumber != -1) {
      true
    } else {
      if (!initialized) initialize
      currentNumber = readNext
      currentNumber != -1
    }
  }

  @inline def readNext: Int = {
    currentNumber = 0
    while (read != -1) {
      while (inputIndex < read) {
        val b = buf(inputIndex)
        inputIndex += 1
        if (!commentedOut) {
          (b: @switch) match {
            case '#' =>
              commentedOut = true
              if (numberStarted) {
                numberStarted = false
                return currentNumber
              }
            case ' ' | '\n' | '\t' =>
              if (numberStarted) {
                numberStarted = false
                return currentNumber
              }
            case other =>
              numberStarted = true
              val number = b - '0'
              assert(number >= 0 && number <= 9, s"Encountered unsupported character $b.")
              currentNumber = 10 * currentNumber + number
          }
        } else if (b == '\n') {
          commentedOut = false
          currentNumber = 0
        }
      }
      inputIndex -= read
      read = in.read(buf)
    }
    in.close
    if (numberStarted) {
      numberStarted = false
      currentNumber
    } else {
      -1
    }
  }

  @inline def initialize {
    in = new BufferedInputStream(new FileInputStream(filePath))
    buf = new Array[Byte](BLOCK_SIZE)
    read = in.read(buf)
    initialized = true
  }
}
