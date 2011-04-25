package signalcollect.implementations.serialization

import java.util.Set
import signalcollect.interfaces._
import util.collections.ConcurrentHashSet
import java.util.Set

abstract class AbstractStorage() extends Storage with DefaultSerializer {

  protected var toCollect = vertexSetFactory //holds all vertex ids that need to signal
  protected var toSignal = vertexSetFactory //holds all vertex ids that need to collect
  protected def vertexSetFactory: Set[Any] = new ConcurrentHashSet[Any](100000, 0.75f, ComputeGraph.defaultNumberOfThreads)
	
  def addForSignling(vertexId: Any) = { toSignal.add(vertexId) }
  def addForCollecting(vertexId: Any) = { toCollect.add(vertexId) }
  def removeFromSignaling(vertexId: Any) = { toSignal.remove(vertexId)}
  def removeFromCollecting(vertexId: Any) = { toCollect.remove(vertexId)}
  def numberOfVerticesToSignal = toSignal.size
  def numberOfVerticesToCollect = toCollect.size

  def hasToSignal = !toSignal.isEmpty
  def hasToCollect = !toCollect.isEmpty
  
  def foreachToSignal[U](f: (Vertex[_, _]) => U) = {
    val i = toSignal.iterator
     while (i.hasNext) {
      f(getVertexWithID(i.next))
    }
    toSignal.clear
  }
  def foreachToCollect[U](f: (Vertex[_, _]) => U, makeSnapShot: Boolean) = {
    if (makeSnapShot) {
      val toCollectSnapshot = toCollect
      toCollect = vertexSetFactory
      val i = toCollectSnapshot.iterator
      while (i.hasNext) {
        f(getVertexWithID(i.next))
      }
    } 
    else {
      val i = toCollect.iterator
      while (i.hasNext) {
        f(getVertexWithID(i.next))
      }
      toCollect.clear
    }
  }

}