/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
 *
 *  Copyright 2011 University of Zurich
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

package com.signalcollect.storage

import com.signalcollect.interfaces._
import scala.collection.mutable.LinkedHashSet
import com.signalcollect.util.collections.ScoredSetWithAverage
import com.signalcollect.Vertex

/**
 * A set to store the toSignal vertex ids that traverses the items with above average scores first.
 */
class AboveAverageVertexIdSet(vertexStore: Storage) extends VertexIdSet {

  protected var implementation: ScoredSetWithAverage[Any] = new ScoredSetWithAverage[Any]()

  override def add(vertexId: Any): Unit = {
    implementation.add(vertexId, vertexStore.vertices.get(vertexId).scoreSignal)
  }

  override def remove(vertexId: Any): Unit = {
    implementation.remove(vertexId)
  }

  def isEmpty: Boolean = implementation.isEmpty

  def size: Int = implementation.size

  def foreach[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    if (!isEmpty) {
      implementation.foreach(f)
      if (removeAfterProcessing) {
        cleanUp
      }
    }
  }

  def applyToNext[U](f: (Any) => U, removeAfterProcessing: Boolean) = {
    if (!isEmpty) {
      val vertexId = implementation.nextAboveAverageItem
      f(vertexId)
      if (removeAfterProcessing) {
        implementation.remove(vertexId)
      }
    }
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) = {
    if (implementation.contains(vertex.id)) {
      implementation.updateItemScore(item = vertex.id, newScore = vertex.scoreSignal)
    }
  }

  def cleanUp = {
    implementation = null
    implementation = new ScoredSetWithAverage[Any]()
  }
}