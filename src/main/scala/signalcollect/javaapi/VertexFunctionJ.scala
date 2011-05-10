package signalcollect.javaapi

import signalcollect.interfaces.Vertex

abstract class VertexFunctionJ extends Function1[Vertex[_, _], Unit] {
  def apply(v: Vertex[_, _]) = doApply(v)
  
  def doApply(v: Vertex[_, _]): Unit
}
 
class PrintVertexJ extends VertexFunctionJ {
   def doApply(v: Vertex[_, _]) {
     println(v)
   }
}