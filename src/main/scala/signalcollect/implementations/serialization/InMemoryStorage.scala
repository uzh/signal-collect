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

class InMemoryStorage(storage: Storage) extends VertexStore {
  val messageBus =  storage.getMessageBus
  var vertexMap = new ConcurrentHashMap[Any, Vertex[_, _]](100000, 0.75f, ComputeGraph.defaultNumberOfThreads)

  def get(id: Any): Vertex[_, _] = {
    vertexMap.get(id)
  }

  def put(vertex: Vertex[_, _]): Boolean = {
    if (!vertexMap.containsKey(vertex.id)) {
      vertex.setMessageBus(messageBus)
      vertexMap.put(vertex.id, vertex)
      storage.toCollect +=vertex.id
      storage.toSignal+=vertex.id
      true
    } else
      false
  }
  def remove(id: Any) = {
    vertexMap.remove(id)
    storage.toCollect-=id
    storage.toSignal-=id
  }

  def updateStateOfVertex(vertex: Vertex[_, _]) = {} // Not needed for in-memory implementation

  def foreach[U](f: (Vertex[_, _]) => U) {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex = it.next
      f(vertex)
    }
  }

  def size: Long = vertexMap.size
}