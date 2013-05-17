/*
 *  @author Daniel Strebel
 *
 *  Copyright 2012 University of Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.signalcollect.examples

import scala.collection.immutable.Queue
import scala.collection.mutable.LinkedList
import scala.collection.mutable.ListBuffer

import com.signalcollect._

/**
 * Elements of a path query.
 * PathQueryNodes represent nodes in a query path that can match for a node in the graph.
 */
abstract class PathQueryNode extends Serializable {
  def matches(vertex: Vertex[_, _]): Boolean
  def expand: List[PathQueryNode] = List()
}

/**
 * PathQueryNode for which the provided condition specifies whether a node matches this query node or not.
 */
class WildcardQueryNode(condition: Vertex[_, _] => Boolean = vertex => true) extends PathQueryNode {
  def matches(vertex: Vertex[_, _]) = condition(vertex)
}

/**
 * More generalized version of WildcardQueryNode that can match 0 to (including) maxExpansion times in a row.
 */
class StarQueryNode(condition: Vertex[_, _] => Boolean = vertex => true, maxExpansion: Int = 1) extends WildcardQueryNode(condition) {
  override def expand: List[PathQueryNode] = {
    if (maxExpansion > 0) {
      List(new StarQueryNode(condition, maxExpansion - 1))
    } else {
      List()
    }
  }
}

/**
 * Query node that only matches a vertex if it has the specified id.
 */
class FixedQueryNode(id: Any) extends PathQueryNode {
  def matches(vertex: Vertex[_, _]) = (vertex.id.hashCode() == id.hashCode())
}

/**
 * A PathQuery is a chain of PathQueryNodes that specify which conditions a path through the graph
 * must fulfill in order to match the query. As the query is passed along matching nodes the matched
 * PathQueryNodes are removed from the active path query and stored in the matched path queue to keep track
 * of the matched nodes in the graph.
 */
class PathQuery() extends Serializable {
  var unmatchedQuery = LinkedList[PathQueryNode]() //Part of the query that is not matched yet.
  var matchedPath = Queue[Any]() // Trail of already matched nodes

  /**
   * Match the head of the query to the provided vertex. If the match was successful a list follow-up queries is returned.
   *
   * @param vertex that should be matched to the head of the remaining path query.
   * @return a list of remaining queries after matching a vertex to the head of the unmatched path query or None if the head did not match the vertex.
   */
  def getRemainingQuery(vertex: Vertex[_, _]): Option[List[PathQuery]] = {
    if (unmatchedQuery.size > 0 && unmatchedQuery.head.matches(vertex)) {
      val remainingQuery = new PathQuery
      remainingQuery.matchedPath = matchedPath.enqueue(vertex.id)
      remainingQuery.unmatchedQuery = unmatchedQuery.tail

      val expandedQueryHeads = unmatchedQuery.head.expand
      val expandedQueries = expandedQueryHeads.map(queryHead => {
        val expandedQuery = new PathQuery
        expandedQuery.matchedPath = remainingQuery.matchedPath
        expandedQuery.prependQueryNode(queryHead)
        expandedQuery
      })
      Some(remainingQuery :: expandedQueries)
    } else {
      None
    }
  }

  /**
   * Adds a PathQueryNode to the end of the unmatched query
   */
  def appendQueryNode(node: PathQueryNode) {
    unmatchedQuery = unmatchedQuery :+ node
  }

  /**
   * Adds a PathQueryNode to the beginning of the unmatched query
   */
  def prependQueryNode(node: PathQueryNode) {
    unmatchedQuery = node +: unmatchedQuery
  }
}

/**
 * Collects all matched paths as results of the query.
 */
object ResultHandler {
  val results = ListBuffer[List[Any]]()
  def addPath(path: List[Any]) = results += path
  def getResults = results.toList
}

/**
 * Collects all matched Paths
 */
class QueryNode

class QueryVertex(vertexId: Int, state: List[PathQuery]) extends DataFlowVertex(vertexId, state) with ResetStateAfterSignaling[Int, List[PathQuery]] {

  type Signal = List[PathQuery]

  val resetState = null

  def collect(queries: List[PathQuery]): List[PathQuery] = {
    var newState = state
    if (queries != null) {
      for (query <- queries) {
        if (query != null) {
          query.getRemainingQuery(this) match {
            case Some(restQueries) => {
              for (restQuery <- restQueries) {
                if (restQuery.unmatchedQuery.size == 0) {
                  ResultHandler.addPath(restQuery.matchedPath.toList)
                } else {
                  if (state != null) {
                    newState = restQuery +: newState
                  } else {
                    newState = List(restQuery)
                  }

                }
              }

            }
            case _ =>
          }
        }
      }
    }
    newState
  }
}

/**
 * A little demo that builds a graph and looks for paths
 */
object PathQueryExample extends App {

  val graph = GraphBuilder.build
  val query = new PathQuery
  //  query.addQueryNode(new WildcardQueryNode)
  //  query.addQueryNode(new WildcardQueryNode)
  //  query.addQueryNode(new FixedQueryNode(3))
  //  query.addQueryNode(new WildcardQueryNode)
  //  query.addQueryNode(new FixedQueryNode(2))
  //  query.addQueryNode(new WildcardQueryNode)
  query.appendQueryNode(new StarQueryNode(maxExpansion = 5))
  query.appendQueryNode(new FixedQueryNode(2))

  graph.addVertex(new QueryVertex(0, List(query)))
  graph.addVertex(new QueryVertex(1, null))
  graph.addVertex(new QueryVertex(2, null))
  graph.addVertex(new QueryVertex(3, null))
  graph.addVertex(new QueryVertex(4, null))
  graph.addVertex(new QueryVertex(5, null))

  graph.addEdge(0, new StateForwarderEdge(1))
  graph.addEdge(0, new StateForwarderEdge(2))
  graph.addEdge(1, new StateForwarderEdge(2))
  graph.addEdge(2, new StateForwarderEdge(3))
  graph.addEdge(3, new StateForwarderEdge(4))
  graph.addEdge(4, new StateForwarderEdge(2))
  graph.addEdge(2, new StateForwarderEdge(5))

  val stats = graph.execute

  println(ResultHandler.getResults)

  graph.shutdown
}