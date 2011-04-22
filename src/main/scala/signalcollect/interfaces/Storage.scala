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

import signalcollect.implementations.serialization.InMemoryStorage



object Storage extends Serializable {
  type StorageFactory = (MessageBus[Any, Any]) => Storage
  lazy val defaultFactory = inMemoryFactory

  def createInMemoryStorage(messageBus: MessageBus[Any, Any]) = new InMemoryStorage(messageBus)
  lazy val inMemoryFactory = createInMemoryStorage _

}

trait Storage {
  def getVertexWithID(id: Any): Vertex[_, _]
  def addVertexToStore(vertex: Vertex[_, _]): Boolean
  def removeVertexFromStore(id: Any)
  def updateStateOfVertex(vertex: Vertex[_, _])
  def getNumberOfVertices: Long
  def foreach[U](f: (Vertex[_, _]) => U)
  
  def addForSignling(vertexId: Any)
  def addForCollecting(vertexId: Any)
  def removeFromSignaling(vertexId: Any)
  def removeFromCollecting(vertexId: Any)
  def hasToSignal: Boolean
  def hasToCollect: Boolean
  def numberOfVerticesToSignal: Long
  def numberOfVerticesToCollect: Long
  def foreachToSignal[U](f: (Vertex[_, _]) => U)
  def foreachToCollect[U](f: (Vertex[_, _]) => U, makeSnapShot: Boolean)
}