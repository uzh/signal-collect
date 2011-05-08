package signalcollect.implementations.serialization

import java.util.Set
import signalcollect.interfaces._
import util.collections.ConcurrentHashSet

class InMemoryVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var toHandle: Set[Any] = vertexSetFactory
  protected var toHandleSnapshot: Set[Any] = vertexSetFactory
  protected def vertexSetFactory = new ConcurrentHashSet[Any](100000, 0.75f, ComputeGraph.defaultNumberOfThreadsUsed)

  def +=(vertexId: Any): Unit = {
    toHandle.add(vertexId)
  }

  def -=(vertexId: Any): Unit = {
    toHandle.remove(vertexId)
  }

  def isEmpty: Boolean = {
    toHandle.isEmpty && toHandleSnapshot.isEmpty
  }

  def size: Long = { toHandle.size }

  def foreach[U](f: (Vertex[_, _]) => U) = {
    val i = toHandle.iterator
    while (i.hasNext) {
      f(vertexStore.vertices.get(i.next))
    }
    toHandle.clear
  }

  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U) = {
    toHandleSnapshot = toHandle
    toHandle = vertexSetFactory
    val i = toHandleSnapshot.iterator
    while (i.hasNext) {
      f(vertexStore.vertices.get(i.next))
    }
    toHandleSnapshot.clear
  }

}