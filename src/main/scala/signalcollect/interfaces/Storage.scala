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

package signalcollect.interfaces

abstract class Storage(messageBus: MessageBus[Any, Any]) {
  def getMessageBus = messageBus
  def vertices: VertexStore
  def toSignal: VertexIdSet //collection of all vertices that need to signal
  def toCollect: VertexIdSet // collection of all vertices that need to collect
}

/**
 * Stores vertices and makes them retrievable through their associated id.
 */
trait VertexStore {  
  def get(id: Any): Vertex[_, _]
  def put(vertex: Vertex[_, _]): Boolean
  def remove(id: Any)
  def updateStateOfVertex(vertex: Vertex[_, _])
  def size: Long
  def foreach[U](f: (Vertex[_, _]) => U)
}

/**
 * Allows storing a set of id and iterating through them
 */
trait VertexIdSet {
  def add(vertexId: Any)
  def remove(vertexId: Any)
  def isEmpty: Boolean
  def size: Long
  def foreach[U](f: (Vertex[_, _]) => U)
  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U, breakConditionReached: () => Boolean): Boolean
  def resumeProcessingSnapshot[U](f: (Vertex[_, _]) => U, breakConditionReached: () => Boolean): Boolean
}
