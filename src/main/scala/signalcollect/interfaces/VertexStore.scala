package signalcollect.interfaces

trait VertexStore {  
  def get(id: Any): Vertex[_, _]
  def put(vertex: Vertex[_, _]): Boolean
  def remove(id: Any)
  def updateStateOfVertex(vertex: Vertex[_, _])
  def size: Long
  def foreach[U](f: (Vertex[_, _]) => U)
}