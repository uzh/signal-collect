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

package signalcollect.implementations.storage

import signalcollect.interfaces._
import com.mongodb.casbah.Imports._

class MongoDBVertexIdSet(vertexStore: Storage) extends VertexIdSet with DefaultSerializer {

  protected var toHandle = vertexSetFactory
  protected def vertexSetFactory = MongoConnection()("todo")(getRandomString("", 16))

  def +=(vertexId: Any): Unit = {
    toHandle += MongoDBObject("k" -> write(vertexId))
  }

  def -=(vertexId: Any): Unit = {
    toHandle.remove(MongoDBObject("k" -> write(vertexId)))
  }

  def isEmpty(): Boolean = {
    toHandle.isEmpty
  }

  def size(): Long = { toHandle.size }

  def foreach[U](f: (Vertex[_, _]) => U) = {
    toHandle.foreach{s => f(vertexStore.vertices.get(read((s.getAs[Array[Byte]]("k")).get))); toHandle.remove(s) 
    }
  }

  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U, breakCondition: () => Boolean):Boolean = {
	  val toHandleSnapshot = toHandle
      toHandle = vertexSetFactory
      toHandleSnapshot.foreach{s => f(vertexStore.vertices.get(read((s.getAs[Array[Byte]]("k")).get))); toHandle.remove(s)}
	  true
  }
  
  def resumeProcessingSnapshot[U](f: (Vertex[_, _]) => U, breakConditionReached: () => Boolean): Boolean = {
	  true
  }
}

trait MongoDBToDoList extends DefaultStorage {
	override protected def vertexSetFactory: VertexIdSet = new MongoDBVertexIdSet(this)
}