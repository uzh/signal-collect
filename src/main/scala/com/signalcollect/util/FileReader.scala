package com.signalcollect.util

import java.io.FileInputStream
import java.io.BufferedInputStream

object FileReader {

  /**
   * Returns an iterator for all Ints in the file at the file path.
   * @Note The numbers need to be positive, fit into a Java Integer,
   * be encoded in ASCII/UTF8 format and can only be separated by
   * single spaces and single newlines.
   */
  def intIterator(filePath: String): Iterator[Int] = {
    new Iterator[Int] {
      var initialized = false
      val BLOCK_SIZE = 8 * 32768
      var in: BufferedInputStream = _
      var currentNumber = 0
      var inputIndex = 0
      var read: Int = _
      var buf: Array[Byte] = _

      def next: Int = {
        currentNumber
      }

      def hasNext: Boolean = {
        if (!initialized) initialize
        currentNumber = readNext
        currentNumber != -1
      }

      def readNext: Int = {
        currentNumber = 0
        while (read != -1) {
          while (inputIndex < read) {
            val b = buf(inputIndex)
            if (b != ' ' && b != '\n') {
              currentNumber = 10 * currentNumber + b - '0'
            } else {
              inputIndex += 1
              return currentNumber
            }
            inputIndex += 1
          }
          inputIndex -= read
          read = in.read(buf)
        }
        in.close
        return -1
      }

      def initialize {
        in = new BufferedInputStream(new FileInputStream(filePath))
        buf = new Array[Byte](BLOCK_SIZE)
        read = in.read(buf)
      }

    }
  }

  /**
   * Sequentially applies the function 'p' to all integers that
   * are read from file 'filePath'.
   * @Note The numbers need to be positive, fit into a Java Integer,
   * be encoded in ASCII/UTF8 format and can only be separated by
   * single spaces and single newlines.
   */
  def processInts(filePath: String, p: Int => Unit) {
    val BLOCK_SIZE = 8 * 32768
    val in = new BufferedInputStream(new FileInputStream(filePath))
    var currentNumber = 0
    var inputIndex = 0
    val buf = new Array[Byte](BLOCK_SIZE)
    var read = in.read(buf)
    while (read != -1) {
      while (inputIndex < read) {
        val b = buf(inputIndex)
        if (b != ' ' && b != '\n') {
          currentNumber = 10 * currentNumber + b - '0'
        } else {
          p(currentNumber)
          currentNumber = 0
        }
        inputIndex += 1
      }
      inputIndex -= read
      read = in.read(buf)
    }
    in.close
  }

}
