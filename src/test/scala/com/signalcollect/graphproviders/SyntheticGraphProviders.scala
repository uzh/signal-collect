package com.signalcollect.graphproviders

/**
 * Required for integration testing. Returns an undirected grid-structured graph.
 * Example Grid(2,2): Edges=(1,3), (3,1), (1,2), (2,1), (2,4), (4,2), (3,4), (4,3)
 * 		1-2
 * 		| |
 * 		3-4
 */
class Grid(val width: Int, height: Int) extends Traversable[(Int, Int)] {

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