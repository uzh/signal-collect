/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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
 */

package com.signalcollect.loading

import com.signalcollect.GraphEditor
import com.signalcollect.Vertex
import com.signalcollect.util.FileReader

object Loading {

  /**
   * Returns a modification iterator that can be used with Graph.loadGraph
   *
   * @param vertexDataIterator Iterator of vertex ID, edges IDs tuples.
   * @param creator Vertex creator that creates a vertex from an (ID, target IDs) tuple.
   */
  @inline def loader[Id, Signal](
    vertexDataIterator: Iterator[(Id, List[Id])],
    creator: (Id, List[Id]) => Vertex[Id, _, Id, Signal]): Iterator[GraphEditor[Id, Signal] => Unit] = {
    vertexDataIterator.map { vData: (Id, List[Id]) =>
      (ge: GraphEditor[Id, Signal]) =>
        val v = creator(vData._1, vData._2)
        ge.addVertex(v)
    }
  }

  /**
   * Returns an iterator that transforms a list of ASCII encoded Int Int tuples that are ordered by source ID into
   * a tuple of the source vertex ID with a list of the respective target IDs.
   * 
   * @note Negative numbers are unsupported.
   */
  @inline def intEdgeIdsOrderedBySourceId(f: String): Iterator[(Int, List[Int])] = {
    new VertexTupleIterator(FileReader.intIterator(f))
  }

}

/**
 * This iterator takes an edge ID iterator that is ordered by source ID and
 * it allows to iterate over the source (vertex ID, edge list) tuples.
 */
final class VertexTupleIterator[Id](val ids: Iterator[Id]) extends Iterator[(Id, List[Id])] {
  private var nextSourceId: Option[Id] = None

  @inline def hasNext: Boolean = {
    if (nextSourceId.isDefined) {
      true
    } else if (ids.hasNext) {
      val nextId = ids.next
      nextSourceId = Some(nextId)
      true
    } else {
      false
    }
  }

  @inline def next: (Id, List[Id]) = {
    val sourceId = nextSourceId.get
    nextSourceId = None
    var targetIds: List[Id] = Nil
    var nextIdIsTargetId = true
    var done = false
    assert(ids.hasNext, s"There was no target ID for edge with source ID $sourceId")
    while (ids.hasNext && !done) {
      val id = ids.next
      if (nextIdIsTargetId) {
        targetIds = id :: targetIds
        nextIdIsTargetId = false
      } else if (id != sourceId) {
        done = true
        nextSourceId = Some(id)
      } else {
        nextIdIsTargetId = true
      }
    }
    (sourceId, targetIds)
  }

}
