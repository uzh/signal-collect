package signalcollect.implementations.messaging

import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.BlockingQueue

// only for SINGLE consumer (and optionally with multiple producers)
// only offers FIFO per producer. which is fine for S/C, as long as a vertex does not migrate from worker to worker
// number of queues has to be a power of 2 so the bitmask works
class MultiQueue[G](queueFactory: () => BlockingQueue[G] = { () => new LinkedBlockingQueue[G]() }, numberOfQueues: Int = Runtime.getRuntime.availableProcessors) extends BlockingQueue[G] {

  private val queues = new Array[BlockingQueue[G]](numberOfQueues)

	private val bitMask = queues.length - 1

  private val putIndex: AtomicInteger = new AtomicInteger(0)
  private var takeIndex = 0

	initializeQueues

	def initializeQueues = {
	  for (i <- 0 until numberOfQueues) {
	    queues(i) = queueFactory()
	  }
  }

  def put(item: G): Unit = {
    val index = putIndex.getAndIncrement & bitMask
    queues(index).put(item)
  }

  def take: G = {
    val index = takeIndex & bitMask
		takeIndex += 1
    val item = queues(index).take
    item
  }
  
  def add(item: G): Boolean = {
	    val index = putIndex.getAndIncrement & bitMask
	    queues(index).put(item)
  		true
  	}
  
  def poll: G = {
    val index = takeIndex & bitMask
    takeIndex += 1
    val result = queues(index).poll()
    if (result == null) {
    		takeIndex -= 1
    }
    result
  }
  
  def poll(timeout: Long, unit: TimeUnit): G = {
    val index = takeIndex & bitMask
    takeIndex += 1
    val item = queues(index).poll(timeout, unit)
    if (item == null) {
    		takeIndex -= 1
    }
    item
  }
  
  def isEmpty: Boolean = {
    takeIndex == putIndex.get
  }
  
  def size: Int = { throw new Exception("Not implemented") }
  def contains(e: Any): Boolean = { throw new Exception("Not implemented") }
  def drainTo(c: java.util.Collection[_ >: G]): Int = { throw new Exception("Not implemented") }
  def drainTo(c: java.util.Collection[_ >: G], maxEle: Int): Int = { throw new Exception("Not implemented") }
  def offer(e: G): Boolean = { throw new Exception("Not implemented") }
  def offer(e: G, timeout: Long, unit: TimeUnit) : Boolean = { throw new Exception("Not implemented") }
  def remainingCapacity: Int = { throw new Exception("Not implemented") }
  def remove(e: Object): Boolean = { throw new Exception("Not implemented") }
  def remove: G = { throw new Exception("Not implemented") }
  def peek: G = { throw new Exception("Not implemented") }
  def clear =  { throw new Exception("Not implemented") }
  def retainAll(x: java.util.Collection[_]): Boolean = { throw new Exception("Not implemented") }
  def removeAll(x: java.util.Collection[_]): Boolean =  { throw new Exception("Not implemented") }
  def addAll(x: java.util.Collection[_ <: G]): Boolean =  { throw new Exception("Not implemented") }
  def containsAll(x: java.util.Collection[_]): Boolean =  { throw new Exception("Not implemented") }
  def toArray[T](x: Array[T with java.lang.Object]): Array[T with java.lang.Object] = { throw new Exception("Not implemented") }
  def toArray: Array[java.lang.Object] = { throw new Exception("Not implemented") }
  def iterator: java.util.Iterator[G] = { throw new Exception("Not implemented") }
  def element: G = { throw new Exception("Not implemented") }
    
}
