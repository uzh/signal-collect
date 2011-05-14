package signalcollect

class Grid(val width: Int, height: Int) extends Traversable[(Int, Int)] {

  def foreach[U](f: ((Int, Int)) => U) = {
	  val max = width*height
	  for (n <- 1 to max) {
	 	if (n + width <= max) {
	 		f(n, n+width)
	 		f(n+width, n)
	 	}
	 	if (n % height != 0) {
	 		f(n, n+1)
	 		f(n+1, n)
	 	}
	  }
  }
}