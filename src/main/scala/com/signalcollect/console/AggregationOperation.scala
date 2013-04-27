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
import com.signalcollect.interfaces.ModularAggregationOperation
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.TopKFinder
import com.signalcollect.Edge
import com.signalcollect.Vertex
import com.signalcollect.interfaces.Inspectable
import BreakConditionName._

class GraphAggregator[Id](nodeIdsList: List[Id] = List[Id](), 
                          vicinityNodeIdsList: List[Id] = List[Id]())
      extends AggregationOperation[JObject] {

  def nodeIds = nodeIdsList.toSet
  def vicinityNodeIds = vicinityNodeIdsList.toSet

  def scanIds(l: Set[Id], l2: Set[Id], category: String, i: Inspectable[Id,_]): JObject = {
    if (l.contains(i.id)) {
      val edges = i.outgoingEdges.values.filter { value =>
        value match {
          case v: Edge[Id] => l.contains(v.targetId) || l2.contains(v.targetId)
          case otherwise => false
        }
      }.map{ e => ( JString(e.targetId.toString))}.toList
      def nodesObj = ("nodes", JObject(List(JField(i.id.toString, 
                        JObject(List(JField("s", i.state.toString),
                                     JField("c", category),
                                     JField("ss", i.scoreSignal),
                                     JField("cs", i.scoreCollect)))))))
      def edgesObj = ("edges", JObject(List(JField(i.id.toString, JArray(edges)))))
      if (edges.size > 0) { nodesObj ~ edgesObj } else { nodesObj }
    }
    else { JObject(List()) }
  }

  def extract(v: Vertex[_,_]): JObject = v match {
    case i: Inspectable[Id,_] => {
      (scanIds(vicinityNodeIds, nodeIds, "v", i)
      merge
      scanIds(nodeIds, vicinityNodeIds, "n", i))
    }
    case other => JObject(List())
  }

  def reduce(vertices: Stream[JObject]): JObject = {
    vertices.foldLeft(JObject(List())) { (acc, v) => 
      acc merge v
    }
  }
}

class SampleAggregator[Id](sampleSize: Int) 
      extends ModularAggregationOperation[List[Id]] {

  val neutralElement = List[Id]()

  def aggregate(a: List[Id], b: List[Id]): List[Id] = {
    val combinedList = a ++ b
    combinedList.slice(0, math.min(sampleSize, combinedList.size))
  }

  def extract(v: Vertex[_, _]): List[Id] = v match {
    case i: Inspectable[Id, _] => 
      List(i.id)
  }
}

class TopDegreeAggregator[Id](n: Int)
      extends AggregationOperation[Map[Id,Int]] {

  def extract(v: Vertex[_, _]): Map[Id,Int] = v match {
    case i: Inspectable[Id, _] => 
      Map(i.id -> i.outgoingEdges.size) ++
      i.outgoingEdges.values.map { 
        case v: Edge[Id] => (v.targetId -> 1)
      }
    case other => Map[Id,Int]()
  }

  def reduce(degrees: Stream[Map[Id,Int]]): Map[Id,Int] = {
    Toolkit.mergeMaps(degrees.toList)((v1, v2) => v1 + v2)
  }
}

class TopStateAggregator[Id](n: Int)
      extends AggregationOperation[List[(Double,Id)]] {

  def extract(v: Vertex[_, _]): List[(Double,Id)] = v match {
    case i: Inspectable[Id, _] => 
      val state: Option[Double] = i.state match {
        case x: Double => Some(x)
        case x: Int => Some(x.toDouble)
        case x: Long => Some(x.toDouble)
        case x: Float => Some(x.toDouble)
        case otherwise => None
      }
      state match {
        case Some(number) => 
          List[(Double,Id)]((number, i.id))
        case otherwise => List[(Double,Id)]()
      }
    case otherwise => List[(Double,Id)]()
  }

  def reduce(degrees: Stream[List[(Double,Id)]]): List[(Double,Id)] = {
    degrees.foldLeft(List[(Double,Id)]()) { (acc, n) => acc ++ n }
           .sortWith({ (t1, t2) => t1._1 > t2._1 })
           .take(n)
  }
}

class FindNodeVicinitiesByIdsAggregator[Id](idsList: List[Id])
      extends AggregationOperation[List[Id]] {

  def ids = idsList.toSet

  def extract(v: Vertex[_,_]): List[Id] = v match {
    case i: Inspectable[Id,_] =>
      // if this node is the target of a primary node, it's a vicinity node
      if(i.outgoingEdges.values.view.map { 
        case v: Edge[Id] if (ids.contains(v.targetId)) => true
        case otherwise => false
      }.toList.contains(true)) { return  List(i.id) }
      // if this node is a primary node, all its targets are vicinity nodes
      if (ids.contains(i.id)) {
        return i.outgoingEdges.values.map{ case v: Edge[Id] => v.targetId }.toList
      }
      // if neither is true, this node is irrelevant
      return List()
    case otherwise => List()
  }

  def reduce(vertices: Stream[List[Id]]): List[Id] = {
    vertices.toList.flatten
  }
}

class FindVerticesByIdsAggregator[Id](idsList: List[String])
      extends AggregationOperation[List[Vertex[Id,_]]] {

  def ids = idsList.toSet

  def extract(v: Vertex[_, _]): List[Vertex[Id,_]] = v match {
    case i: Inspectable[Id, _] => {
      if (ids.contains(i.id.toString)) { return List(i) }
      else { return List() }
    }
    case other => List()
  }

  def reduce(vertices: Stream[List[Vertex[Id,_]]]): List[Vertex[Id,_]] = {
    vertices.toList.flatten
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
      }
      results
    }
  }
  def reduce(results: Stream[Map[String,String]]): Map[String,String] = {
    Toolkit.mergeMaps(results.toList)((v1, v2) => v1 + v2)
  }
}
