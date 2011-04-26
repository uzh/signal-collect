package signalcollect.interfaces

trait VertexIdSet {
  def +=(vertexId: Any)
  def -=(vertexId: Any)
  def isEmpty: Boolean
  def size: Long
  def foreach[U](f: (Vertex[_, _]) => U)
  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U)
}