package com.signalcollect.graphproviders

/**
 * Required for integration testing. Returns an undirected grid-structured graph.
 * Example Grid(2,2): Edges=(1,3), (3,1), (1,2), (2,1), (2,4), (4,2), (3,4), (4,3)
 * 		1-2
 * 		| |
 * 		3-4
 */
class Grid(val width: Int, height: Int) extends Traversable[(Int, Int)] with Serializable {

  def foreach[U](f: ((Int, Int)) => U) = {
    val max = width * height
    for (n <- 1 to max) {
      if (n + width <= max) {
        f(n, n + width)
        f(n + width, n)
      }
      if (n % height != 0) {
        f(n, n + 1)
        f(n + 1, n)
      }
    }
  }
}


class Torus(val width: Int, height: Int) extends Traversable[(Int, Int)] with Serializable {

  def foreach[U](f: ((Int, Int)) => U) = {
    val max = width * height
    for (y <- 0 until height) {
      for (x <- 0 until width) {
        val flattenedCurrentId = flatten((x, y), width)
        for (neighbor <- neighbors(x, y, width, height).map(flatten(_, width))) {
          f(flattenedCurrentId, neighbor)
        }
      }
    }
  }

  //  def neighbors(x: Int, y: Int, width: Int, height: Int): List[(Int, Int)] = {
  //    List(
  //      (decrease(x, width), decrease(y, height)), (x, decrease(y, height)), (increase(x, width), decrease(y, height)),
  //      (decrease(x, width), y), (increase(x, width), y),
  //      (decrease(x, width), increase(y, height)), (x, increase(y, height)), (increase(x, width), increase(y, height)))
  //  }

  def neighbors(x: Int, y: Int, width: Int, height: Int): List[(Int, Int)] = {
    List(
      (x, decrease(y, height)),
      (decrease(x, width), y), (increase(x, width), y),
      (x, increase(y, height)))
  }

  def decrease(counter: Int, limit: Int): Int = {
    if (counter - 1 >= 0) {
      counter - 1
    } else {
      width - 1
    }
  }

  def increase(counter: Int, limit: Int): Int = {
    if (counter + 1 >= width) {
      0
    } else {
      counter + 1
    }
  }

  def flatten(coordinates: (Int, Int), width: Int): Int = {
    coordinates._1 + coordinates._2 * width
  }
}