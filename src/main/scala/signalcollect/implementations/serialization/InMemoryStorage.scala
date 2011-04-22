/*
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

package signalcollect.implementations.serialization

import util.collections.ConcurrentHashSet
import java.util.Set
import signalcollect.interfaces._
import java.util.concurrent.ConcurrentHashMap

class InMemoryStorage(messageBus: MessageBus[Any, Any]) extends Storage {

  var vertexMap = new ConcurrentHashMap[Any, Vertex[_, _]](100000, 0.75f, ComputeGraph.defaultNumberOfThreads)
  protected var toCollect = vertexSetFactory //holds all vertex ids that need to signal
  protected var toSignal = vertexSetFactory //holds all vertex ids that need to collect
  protected def vertexSetFactory: Set[Any] = new ConcurrentHashSet[Any](100000, 0.75f, ComputeGraph.defaultNumberOfThreads)

  def getVertexWithID(id: Any): Vertex[_, _] = {
    vertexMap.get(id)
  }

  def addVertexToStore(vertex: Vertex[_, _]): Boolean = {
    if (!vertexMap.containsKey(vertex.id)) {
      vertex.setMessageBus(messageBus)
      vertexMap.put(vertex.id, vertex)
      addForCollecting(vertex.id)
      addForSignling(vertex.id)
      true
    } else
      false
  }
  def removeVertexFromStore(id: Any) = {
    vertexMap.remove(id)
    removeFromCollecting(id)
    removeFromSignaling(id)
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) = {} // Not needed for in-memory implementation

  def foreach[U](f: (Vertex[_, _]) => U) {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex = it.next
      f(vertex)
    }
  }

  def getNumberOfVertices: Long = vertexMap.size

  /*
   * toSignal & toCollect methods
   */

  def addForSignling(vertexId: Any) = { toSignal.add(vertexId) }
  def addForCollecting(vertexId: Any) = { toCollect.add(vertexId) }
  def removeFromSignaling(vertexId: Any) = { toSignal.remove(vertexId) }
  def removeFromCollecting(vertexId: Any) = { toCollect.remove(vertexId) }
  def numberOfVerticesToSignal = toSignal.size
  def numberOfVerticesToCollect = toCollect.size

  def hasToSignal = !toSignal.isEmpty
  def hasToCollect = !toCollect.isEmpty

  def foreachToSignal[U](f: (Vertex[_, _]) => U) = {
    val i = toSignal.iterator
    while (i.hasNext) {
      f(vertexMap.get(i.next))
    }
    toSignal.clear
  }
  def foreachToCollect[U](f: (Vertex[_, _]) => U, makeSnapShot: Boolean) = {
    if (makeSnapShot) {
      val toCollectSnapshot = toCollect
      toCollect = vertexSetFactory
      val i = toCollectSnapshot.iterator
      while (i.hasNext) {
        f(vertexMap.get(i.next))
      }
    } 
    else {
      val i = toCollect.iterator
      while (i.hasNext) {
        f(vertexMap.get(i.next))
      }
      toCollect.clear
    }
  }
}