/*
 *  @author Carol Alexandru
 *  
 *  Copyright 2013 University of Zurich
 *      
 *  Licensed below the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed below the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations below the License.
 *  
 */

package com.signalcollect.console

import scala.language.postfixOps
import com.signalcollect.interfaces.AggregationOperation
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.TopKFinder
import com.signalcollect.Vertex
import com.signalcollect.interfaces.Inspectable
import BreakConditionName._

class GraphAggregator(nodeIds: List[String] = List[String](), 
                      vicinityNodeIds: List[String] = List[String]())
      extends AggregationOperation[JObject] {

  def scanIds(l: List[String], category: String, i: Inspectable[_,_]): JObject = {
    if (l.contains(i.id.toString)) {
      val edges = i.outgoingEdges.values.filter { 
        v => l.contains(v.targetId.toString)
      }
      JObject(List(
        JField("nodes", JObject(List(JField(i.id.toString, 
                        JObject(List(JField("s", i.state.toString),
                                     JField("c", category),
                                     JField("ss", i.scoreSignal),
                                     JField("cs", i.scoreCollect))))))),
        JField("edges", JObject(List(JField(i.id.toString, JArray(
          edges.map{ e => ( JString(e.targetId.toString))}.toList)))))
      ))
    }
    else { JObject(List()) }
  }

  def extract(v: Vertex[_, _]): JObject = v match {
    case i: Inspectable[_, _] => {
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

class TopDegreeAggregator(n: Int)
      extends AggregationOperation[Map[String,Int]] {

  def extract(v: Vertex[_, _]): Map[String,Int] = v match {
    case i: Inspectable[_, _] => 
      Map(i.id.toString -> i.outgoingEdges.size) ++
      (for(v <- i.outgoingEdges.values) yield (v.targetId.toString -> 1)).toMap
    case other => Map[String,Int]()
  }

  def reduce(degrees: Stream[Map[String,Int]]): Map[String,Int] = {
    Toolkit.mergeMaps(degrees.toList)((v1, v2) => v1 + v2)
  }
}

class TopStateAggregator(n: Int)
      extends AggregationOperation[List[(Double,String)]] {

  def extract(v: Vertex[_, _]): List[(Double,String)] = v match {
    case i: Inspectable[_, _] => 
      val state: Option[Double] = try { Some(i.state.toString.toDouble) } 
                                  catch { case _: Throwable => None }
      state match {
        case Some(number) => 
          List[(Double,String)]((number, i.id.toString))
        case otherwise => List[(Double,String)]()
      }
    case otherwise => List[(Double,String)]()
  }

  def reduce(degrees: Stream[List[(Double,String)]]): List[(Double,String)] = {
    degrees.foldLeft(List[(Double,String)]()) { (acc, n) => acc ++ n }
           .sortWith({ (t1, t2) => t1._1 < t2._1 })
           .take(n)
  }
}

class FindNodeVicinitiesByIdsAggregator(ids: List[String])
      extends AggregationOperation[Map[String,List[String]]] {
  def extract(v: Vertex[_,_]): Map[String,List[String]] = v match {
    case i: Inspectable[_,_] =>
      (((ids.contains(i.id.toString)) match {
        case true => 
          Map(i.id.toString -> i.outgoingEdges.values.map{_.targetId.toString}.toList)
        case otherwise =>
          Map()
        })
      ++
      (for(v <- i.outgoingEdges.values if (ids.contains(v.targetId.toString)))
       yield (v.targetId.toString -> List(i.id.toString))).toMap)

    case otherwise => 
      Map()
  }

  def reduce(vertices: Stream[Map[String,List[String]]]): Map[String,List[String]] = {
    Toolkit.mergeMaps(vertices.toList)((v1, v2) => v1 ++ v2)
  }
  /*def extract(v: Vertex[_, _]): Map[String,Int] = v match {
    case i: Inspectable[_, _] => 
      Map(i.id.toString -> i.outgoingEdges.size) ++
      (for(v <- i.outgoingEdges.values) yield (v.targetId.toString -> 1)).toMap
    case other => Map[String,Int]()
  }

  def reduce(degrees: Stream[Map[String,Int]]): Map[String,Int] = {
    Toolkit.mergeMaps(degrees.toList)((v1, v2) => v1 + v2)
  }*/

}
class FindVertexByIdAggregator(id: String)
      extends AggregationOperation[Option[Vertex[_,_]]] {
  def extract(v: Vertex[_, _]): Option[Vertex[_,_]] = v match {
    case i: Inspectable[_, _] => {
      if (i.id.toString == id) { return Some(i) }
      else { return None }
    }
    case other => None
  }

  def reduce(vertices: Stream[Option[Vertex[_,_]]]): Option[Vertex[_,_]] = {
    vertices.flatten.headOption
  }

}

class BreakConditionsAggregator(conditions: Map[String,BreakCondition]) 
      extends AggregationOperation[Map[String,String]] {

  val nodeConditions = List(
    ChangesState,
    GoesAboveState,
    GoesBelowState,
    GoesAboveSignalThreshold,
    GoesBelowSignalThreshold,
    GoesAboveCollectThreshold,
    GoesBelowCollectThreshold
  )

  val allNodeConditions = List(
  )

  def extract(v: Vertex[_, _]): Map[String,String] = v match {
    case i: Inspectable[_, _] => {
      var results = Map[String,String]()
      conditions.foreach { case (id, c) => 
        // conditions that need to be checked on a specific node
        if (nodeConditions.contains(c.name)) {
          if (i.id.toString == c.props("nodeId")) {
            c.name match { 
              case ChangesState =>
                if (i.state.toString != c.props("currentState"))
                  results += (id -> i.state.toString)
              case GoesAboveState =>
                if (i.state.toString.toDouble > c.props("expectedState").toDouble)
                  results += (id -> i.state.toString)
              case GoesBelowState =>
                if (i.state.toString.toDouble < c.props("expectedState").toDouble)
                  results += (id -> i.state.toString)
              case GoesBelowSignalThreshold =>
                if (i.scoreSignal < c.props("threshold").toDouble)
                  results += (id -> i.scoreSignal.toString)
              case GoesAboveSignalThreshold =>
                if (i.scoreSignal > c.props("threshold").toDouble)
                  results += (id -> i.scoreSignal.toString)
              case GoesBelowCollectThreshold =>
                if (i.scoreCollect < c.props("threshold").toDouble)
                  results += (id -> i.scoreCollect.toString)
              case GoesAboveCollectThreshold =>
                if (i.scoreCollect > c.props("threshold").toDouble)
                  results += (id -> i.scoreCollect.toString)
            }
          }
        }
        // conditions that need to be checked on every node
        // else if (allNodeConditions.contains(c.name)) {  }
      }
      results
    }
  }
  def reduce(results: Stream[Map[String,String]]): Map[String,String] = {
    Toolkit.mergeMaps(results.toList)((v1, v2) => v1 + v2)
  }
}
