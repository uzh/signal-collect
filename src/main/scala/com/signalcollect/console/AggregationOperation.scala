package com.signalcollect.console

import com.signalcollect.interfaces.AggregationOperation
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.TopKFinder
import com.signalcollect.Vertex
import com.signalcollect.interfaces.Inspectable
import scalaz.Scalaz._

class GraphAggregator[Id](ids: List[Id] = List[Id]())
      extends AggregationOperation[JObject] {

  val all = ids.size match {
    case n if(n > 0) => false
    case otherwise => true
  }

  def extract(v: Vertex[_, _]): JObject = v match {
    case i: Inspectable[Id, _] => {
      if (all || ids.contains(i.id)) {
        val edges = i.outgoingEdges.values.filter { 
          v => all || ids.contains(v.targetId)
        }
        JObject(List(
          JField("nodes", JObject(List(JField(i.id.toString, i.state.toString)))),
          JField("edges", JObject(List(JField(i.id.toString, JArray(
            edges.map{ e => ( JString(e.targetId.toString))}.toList)))))
        ))
      }
      else { JObject(List()) }
    }
    case other => JObject(List())
  }

  def reduce(vertices: Stream[JObject]): JObject = {
    vertices.foldLeft(JObject(List())) { (acc, v) => 
      acc merge v
    }
  }
}

class TopDegreeAggregator[Id](maxNodes: Int = 3)
      extends AggregationOperation[Map[Int,List[Id]]] {

  def extract(v: Vertex[_, _]): Map[Int,List[Id]] = v match {
    case i: Inspectable[Id, _] => 
      collection.immutable.TreeMap[Int,List[Id]](
        i.outgoingEdges.size -> List[Id](i.id)
      )(implicitly[Ordering[Int]].reverse)
    case other => Map[Int,List[Id]]()
  }

  def reduce(degrees: Stream[Map[Int,List[Id]]]): Map[Int,List[Id]] = {
    val result = degrees.foldLeft(Map[Int,List[Id]]()) { (acc, m) => 
      val merged = m.foldLeft(Map[Int,List[Id]]()) { (accL, l) =>
        accL |+| Map[Int,List[Id]](l._1 -> l._2)
      }
      acc |+| merged
    }
    result
  }
}

class FindVertexByIdAggregator[Id](id: String)
      extends AggregationOperation[Option[Vertex[Id,_]]] {
  def extract(v: Vertex[_, _]): Option[Vertex[Id,_]] = v match {
    case i: Inspectable[Id, _] => {
      if (i.id.toString == id) { return Some(i) }
      else { return None }
    }
    case other => None
  }

  def reduce(vertices: Stream[Option[Vertex[Id,_]]]): Option[Vertex[Id,_]] = {
    vertices.flatten.headOption
  }

}

