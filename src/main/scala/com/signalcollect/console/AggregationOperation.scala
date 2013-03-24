package com.signalcollect.console

import com.signalcollect.interfaces.AggregationOperation
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.TopKFinder
import com.signalcollect.Vertex
import com.signalcollect.interfaces.Inspectable

class GraphAggregator[Id](nodeIds: List[Id] = List[Id](), vicinityNodeIds: List[Id] = List[Id]())
      extends AggregationOperation[JObject] {

  def scanIds(l: List[Id], category: String, i: Inspectable[Id,_]): JObject = {
    if (l.contains(i.id)) {
      val edges = i.outgoingEdges.values.filter { 
        v => l.contains(v.targetId)
      }
      JObject(List(
        JField("nodes", JObject(List(JField(i.id.toString, 
                        JObject(List(JField("s", i.state.toString),
                                     JField("c", category))))))),
        JField("edges", JObject(List(JField(i.id.toString, JArray(
          edges.map{ e => ( JString(e.targetId.toString))}.toList)))))
      ))
    }
    else { JObject(List()) }
  }

  def extract(v: Vertex[_, _]): JObject = v match {
    case i: Inspectable[Id, _] => {
      scanIds(vicinityNodeIds, "v", i) merge scanIds(nodeIds, "n", i) 
    }
    case other => JObject(List())
  }

  def reduce(vertices: Stream[JObject]): JObject = {
    vertices.foldLeft(JObject(List())) { (acc, v) => 
      acc merge v
    }
  }
}

class TopDegreeAggregator[Id]()
      extends AggregationOperation[Map[Int,List[Id]]] {

  def extract(v: Vertex[_, _]): Map[Int,List[Id]] = v match {
    case i: Inspectable[Id, _] => 
      collection.immutable.TreeMap[Int,List[Id]](
        i.outgoingEdges.size -> List[Id](i.id)
      )(implicitly[Ordering[Int]].reverse)
    case other => Map[Int,List[Id]]()
  }

  def reduce(degrees: Stream[Map[Int,List[Id]]]): Map[Int,List[Id]] = {
    Toolkit.mergeMaps(degrees.toList)((v1, v2) => v1 ++ v2)
  }
}

class TopStateAggregator[Id]()
      extends AggregationOperation[Map[Double,List[Id]]] {

  def extract(v: Vertex[_, _]): Map[Double,List[Id]] = v match {
    case i: Inspectable[Id, _] => 
      val state: Option[Double] = try { Some(i.state.toString.toDouble) } 
                                  catch { case _: Throwable => None }
      state match {
        case Some(number) => 
          collection.immutable.TreeMap[Double,List[Id]](
            number -> List[Id](i.id)
          )(implicitly[Ordering[Double]].reverse)
        case otherwise => Map[Double,List[Id]]()
      }
    case otherwise => Map[Double,List[Id]]()
  }

  def reduce(degrees: Stream[Map[Double,List[Id]]]): Map[Double,List[Id]] = {
    Toolkit.mergeMaps(degrees.toList)((v1, v2) => v1 ++ v2)
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

