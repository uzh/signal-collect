/**
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
 */

package com.signalcollect.console

import scala.language.postfixOps
import com.signalcollect.DataGraphVertex
import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.interfaces.ModularAggregationOperation
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.TopKFinder
import com.signalcollect.Edge
import com.signalcollect.Vertex
import com.signalcollect.interfaces.Inspectable
import BreakConditionName._

/**
 * Aggregator that loads a JObject representation of vertices and their edges.
 *
 * Given the set of IDs, the aggregator retrieves the corresponding vertices and the
 * edges between the vertices. The aggregator returns a JObject, which contains
 * two objects, one for vertices, one for edges. The data structure is best
 * explained by an example:
 *
 * {{{
 * {"vertices":{"id1":{"s":"0.15","ss":0.0,"cs":1.0},
 *           "id2":{"s":"0.16","ss":1.0,"cs":1.0},
 *           "id3":{"s":"0.17","ss":1.0,"cs":1.0}},
 *  "edges":{"id1":["id2","id3"]}}}
 * }}}
 *
 * The vertices object uses a vertex id as key and stores the state, signal and
 * collect scores. The edges object uses a vertex id as key and stores the list
 * of target vertices.
 *
 * @constructor create the aggregator
 * @param vertexIds set of vertex IDs to be loaded
 * @param exposeVertices whether or not to call expose() on each vertex and put
 *     the return value into the "info" property. If true, may increase
 *     graph loading time significantly.
 */
class GraphAggregator[Id](vertexIds: Set[Id] = Set[Id](), exposeVertices: Boolean = false)
    extends AggregationOperation[(Double, Double, JObject)] {

  def interpretState(s: Any): Double = {
    s match {
      case x: Double => x
      case x: Int    => x.toDouble
      case x: Long   => x.toDouble
      case x: Float  => x.toDouble
      case otherwise => 0.0
    }
  }

  def extract(v: Vertex[_, _]): ((Double, Double, JObject)) = {
      def state: Double = interpretState(v.state)
    v match {
      case i: Inspectable[Id, _] => {
        if (vertexIds.contains(i.id)) {
          // Get the list of target vertices that this vertex' edges point at
          val targetVertices = i.edges.filter { value =>
            // This match is necessary because only an Edge[Id] will have a
            // targetId of type Id.
            value match {
              case v: Edge[Id] => vertexIds.contains(v.targetId)
              case otherwise   => false
            }
          }.map { e => (JString(e.targetId.toString)) }.toList

          val stateString = i.state match {
            case null  => "null"
            case other => other.toString
          }
            def vertexProperties: List[JField] = (List(JField("s", stateString),
              JField("es", targetVertices.size),
              JField("ss", i.scoreSignal),
              JField("cs", i.scoreCollect),
              JField("t", v.getClass.toString.split("""\.""").last),
              JField("info",
                if (exposeVertices) {
                  JObject(
                    (for ((k, v) <- i.expose) yield {
                      JField(k, Toolkit.serializeAny(v))
                    }).toList)
                } else { JNothing })))
            def verticesObj: (String, JObject) = ("vertices",
              JObject(List(JField(i.id.toString, JObject(vertexProperties)))))

            def edgesObj: (String, JObject) = ("edges", JObject(List(JField(i.id.toString, JArray(targetVertices)))))
          (state, state, verticesObj ~ edgesObj)
        } else { (state, state, JObject(List())) }

      }
      case other => (state, state, JObject(List()))
    }
  }

  def reduce(subGraphs: Stream[(Double, Double, JObject)]): (Double, Double, JObject) = {
    // Determine the lowest and highest state and merge the sub-graphs
    subGraphs.size match {
      case 0 => (0.0, 0.0, JObject(List()))
      case otherwise =>
        subGraphs.foldLeft((subGraphs.head._1, subGraphs.head._2, JObject(List()))) { (acc, v) =>
          (if (acc._1 < v._1) acc._1 else v._1,
            if (acc._2 > v._2) acc._2 else v._2,
            acc._3 merge v._3)
        }
    }
  }
}

/**
 * Aggregator that retrieves a random sample of vertex IDs.
 *
 * @constructor create the aggregator
 * @param sampleSize the number of vertex IDs to retrieve
 */
class SampleAggregator[Id](sampleSize: Int)
    extends ModularAggregationOperation[Set[Id]] {

  val neutralElement = Set[Id]()

  def aggregate(a: Set[Id], b: Set[Id]): Set[Id] = {
    val combinedSet = a ++ b
    combinedSet.slice(0, math.min(sampleSize, combinedSet.size)).toSet
  }

  def extract(v: Vertex[_, _]): Set[Id] = v match {
    case i: Inspectable[Id, _] =>
      List(i.id).toSet
  }
}

/**
 * Aggregator that retrieves vertices with the highest degree.
 *
 * The aggregator produces a map of vertex IDs to degrees.
 *
 * @constructor create the aggregator
 * @param n the number of top elements to find
 */
class TopDegreeAggregator[Id](n: Int)
    extends AggregationOperation[Map[Id, Int]] {

  def extract(v: Vertex[_, _]): Map[Id, Int] = v match {
    case i: Inspectable[Id, _] =>
      // Create one map from this id to the number of outgoing edges
      Map(i.id -> i.edges.size) ++
        // Create several maps, one for each target id to 1
        i.edges.map {
          case v: Edge[Id] => (v.targetId -> 1)
        }
    case other => Map[Id, Int]()
  }

  def reduce(vertexToDegreeMap: Stream[Map[Id, Int]]): Map[Id, Int] = {
    // Combine the maps created above to count the total number of edges
    Toolkit.mergeMaps(vertexToDegreeMap.toList)((v1, v2) => v1 + v2)
  }
}

/**
 * Aggregator that retrieves vertices with the highest or lowest state.
 *
 * The aggregator produces a list of tuples, each containing the state and the
 * vertex ID with that state.
 *
 * @constructor create the aggregator
 * @param n the number of top elements to find
 * @param inverted gather by lowest, not highest state
 */
class TopStateAggregator[Id](n: Int, inverted: Boolean)
    extends AggregationOperation[List[(Double, Id)]] {

  def extract(v: Vertex[_, _]): List[(Double, Id)] = v match {
    case i: Inspectable[Id, _] =>
      // Try to interpret different types of numberic states
      val state: Option[Double] = i.state match {
        case x: Double => Some(x)
        case x: Int    => Some(x.toDouble)
        case x: Long   => Some(x.toDouble)
        case x: Float  => Some(x.toDouble)
        case otherwise => None
      }
      state match {
        case Some(number) =>
          List[(Double, Id)]((number, i.id))
        case otherwise => List[(Double, Id)]()
      }
    case otherwise => List[(Double, Id)]()
  }

  def reduce(statesAndIds: Stream[List[(Double, Id)]]): List[(Double, Id)] = {
    // Sort the tuples by descending/ascending value and take the first n tuples
    statesAndIds.foldLeft(List[(Double, Id)]()) { (acc, n) => acc ++ n }
      .sortWith({ (t1, t2) =>
        if (inverted) { t1._1 < t2._1 }
        else { t1._1 > t2._1 }
      })
      .take(n)
  }
}

/**
 * Aggregator that retrieves vertices with the highest signal or collect scores.
 *
 * The aggregator produces a list of tuples, each containing the score and the
 * vertex ID with that score.
 *
 * @constructor create the aggregator
 * @param n the number of top elements to find
 * @param scoreType the score to look at (signal or collect)
 */
class AboveThresholdAggregator[Id](n: Int, scoreType: String, threshold: Double)
    extends AggregationOperation[List[(Double, Id)]] {

  def extract(v: Vertex[_, _]): List[(Double, Id)] = v match {
    case i: Inspectable[Id, _] =>
      val score = scoreType match {
        case "signal"  => i.scoreSignal
        case "collect" => i.scoreCollect
      }
      // Yield score and id only if the score is above the threshold
      if (score > threshold) {
        List[(Double, Id)]((score, i.id))
      } else {
        List[(Double, Id)]()
      }
    case otherwise => List[(Double, Id)]()
  }

  def reduce(thresholdsAndIds: Stream[List[(Double, Id)]]): List[(Double, Id)] = {
    // Sort the tuples by descending thresholds and take the first n tuples
    thresholdsAndIds.foldLeft(List[(Double, Id)]()) { (acc, n) => acc ++ n }
      .sortWith({ (t1, t2) => t1._1 > t2._1 })
      .take(n)
  }

}

/**
 * Aggregator that loads the IDs of vertices in the vicinity of other vertices.
 *
 * The aggregator produces a new set of IDs representing the vertices that are
 * connected to any of the vertices in the given set, be it incoming or outgoing.
 *
 * @constructor create the aggregator
 * @param ids set of vertex IDs to be loaded
 */
class FindVertexVicinitiesByIdsAggregator[Id](ids: Set[Id])
    extends AggregationOperation[Set[Id]] {

  def extract(v: Vertex[_, _]): Set[Id] = v match {
    case i: Inspectable[Id, _] =>
      // If this vertex is the target of a primary vertex, it's a vicinity vertex
      if (i.edges.view.map {
        case v: Edge[Id] if (ids.contains(v.targetId)) => true
        case otherwise                                 => false
      }.toSet.contains(true)) { return Set(i.id) }
      // If this vertex is a primary vertex, all its targets are vicinity vertices
      if (ids.contains(i.id)) {
        return i.edges.map { case v: Edge[Id] => v.targetId }.toSet
      }
      // If neither is true, this vertex is irrelevant
      return Set()
    case otherwise => Set()
  }

  def reduce(vertexIds: Stream[Set[Id]]): Set[Id] = {
    vertexIds.toSet.flatten
  }
}

/**
 * Aggregator that translates a list of strings to a list of vertices.
 *
 * The aggregator compares the string representation of the id of any vertex
 * to the strings supplied to it.
 *
 * @constructor create the aggregator
 * @param idsList the list of IDs to compare vertex IDs with
 */
class FindVerticesByIdsAggregator[Id](idsList: List[String])
    extends AggregationOperation[List[Vertex[Id, _]]] {

  def ids: Set[String] = idsList.toSet

  def extract(v: Vertex[_, _]): List[Vertex[Id, _]] = v match {
    case i: Inspectable[Id, _] => {
      if (ids.contains(i.id.toString)) { List(i) }
      else { List() }
    }
    case other => List()
  }

  def reduce(vertices: Stream[List[Vertex[Id, _]]]): List[Vertex[Id, _]] = {
    vertices.toList.flatten
  }

}

/**
 * Aggregator that finds a list of node IDs which contain a given substring.
 *
 * @constructor create the aggregator
 * @param s the substring that should be contained in the node ID
 * @param limit maximum number of nodes to find
 */
class FindVertexIdsBySubstringAggregator[Id](s: String, limit: Int)
    extends ModularAggregationOperation[Set[Id]] {

  val neutralElement = Set[Id]()

  def extract(v: Vertex[_, _]): Set[Id] = v match {
    case i: Inspectable[Id, _] => {
      if (i.id.toString.contains(s)) { Set(i.id) }
      else { Set() }
    }
    case other => Set()
  }

  def aggregate(a: Set[Id], b: Set[Id]): Set[Id] = {
    val combinedSet = a ++ b
    combinedSet.slice(0, math.min(limit, combinedSet.size)).toSet
  }
}

/**
 * Aggregator that checks if any of the break conditions apply
 *
 * The aggregator takes a map of IDs (strings used to identify break
 * conditions) to BreakCondition items. It produces a map of the same IDs to
 * strings which represent the reason for the condition firing. For example,
 * one result item may be ("3" -> "0.15"), which would mean that the condition
 * identified as "3" fired because of a value "0.15". Depending on the state,
 * not all condition checks are performed. For example, the signal threshold
 * is only ever checked before the signaling step, because else it will be 0
 * anyway.
 *
 * @constructor create the aggregator
 * @param conditions map of conditions
 * @param state string denoting the current state
 */
class BreakConditionsAggregator(conditions: Map[String, BreakCondition], state: String)
    extends AggregationOperation[Map[String, String]] {

  val relevantVertexIds = conditions.map(_._2.props("vertexId")).toSet

  def extract(v: Vertex[_, _]): Map[String, String] = v match {
    case i: Inspectable[_, _] => {
      var results = Map[String, String]()
      if (relevantVertexIds.contains(i.id.toString)) {
        conditions.foreach {
          case (id, c) =>
            if (i.id.toString == c.props("vertexId")) {
              // It depends on the state which checks are performed, because
              // some checks would falsely return true during some states. For
              // example, checking the signal scores right after a signal step
              // would yield a value below the threshold every time. In this
              // case, we only check the signal scores after a collect step.
              state match {
                case "checksAfterSignal" => c.name match {
                  case CollectScoreBelowThreshold =>
                    if (i.scoreCollect < c.props("collectThreshold").toDouble) {
                      results += (id -> i.scoreCollect.toString)
                    }
                  case CollectScoreAboveThreshold =>
                    if (i.scoreCollect > c.props("collectThreshold").toDouble) {
                      results += (id -> i.scoreCollect.toString)
                    }
                  case otherwise =>
                }
                case "checksAfterCollect" => c.name match {
                  case StateChanges =>
                    if (i.state.toString != c.props("currentState")) {
                      results += (id -> i.state.toString)
                    }
                  case StateAbove =>
                    if (i.state.toString.toDouble > c.props("expectedState").toDouble) {
                      results += (id -> i.state.toString)
                    }
                  case StateBelow =>
                    if (i.state.toString.toDouble < c.props("expectedState").toDouble) {
                      results += (id -> i.state.toString)
                    }
                  case SignalScoreBelowThreshold =>
                    if (i.scoreSignal < c.props("signalThreshold").toDouble) {
                      results += (id -> i.scoreSignal.toString)
                    }
                  case SignalScoreAboveThreshold =>
                    if (i.scoreSignal > c.props("signalThreshold").toDouble) {
                      results += (id -> i.scoreSignal.toString)
                    }
                  case otherwise =>
                }
              }
            }
        }
      }
      results
    }
  }
  def reduce(results: Stream[Map[String, String]]): Map[String, String] = {
    Toolkit.mergeMaps(results.toList)((v1, v2) => v1 + v2)
  }
}

