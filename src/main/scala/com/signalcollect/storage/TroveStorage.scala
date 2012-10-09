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
 */

package com.signalcollect.storage

import java.util.Set
import java.util.HashMap
import com.signalcollect._
import scala.collection.JavaConversions
import gnu.trove.map.hash.TIntObjectHashMap
import com.signalcollect.interfaces._

/**
 * Stores all vertices in a in-memory Trove-HashMap.
 * HashMap uses the id of the vertices as the hash key.
 * If the ID is of type Int it assumes that this integer is globally unique and uses it as the key
 * otherwise the hashCode of the vertex's id is used which could potentially lead to collisions. 
 */
class TroveStorage(storage: Storage) extends VertexStore {
  protected var vertexMap = new TIntObjectHashMap[Vertex[_,_]]()
  
  def getIntForId(id: Any): Int = {
    id match {
      case intId: Int => return intId
      case _  => return id.hashCode
    }  
  }

  /**
   * Returns a vertex from the store that has the specified id.
   *
   * @param id the ID of the vertex to retrieve
   * @return the vertex object or null if the vertex is not contained in the store
   */
  def get(id: Any): Vertex[_, _] = {
    vertexMap.get(getIntForId(id))
  }

  /**
   * Filter vertices that satisfy a condition
   *
   * @param condition that has to be met by a vertex so that it will be returned.
   */
  def getAll(condition: Vertex[_, _] => Boolean): List[Vertex[_, _]] = {
	  var result = List[Vertex[_, _]]()
	  var iter = vertexMap.values.iterator
	  while(iter.hasNext) {
	    val candidateVertex = iter.next.asInstanceOf[Vertex[_,_]]
	    if(condition(candidateVertex)) {
	      result = candidateVertex::result
	    }
	  }
	  result
  }

  /**
   * Inserts a vertex in the collection if the vertex collection does not already contain a vertex with the same id.
   *
   * @param the vertex to insert
   * @return true if the insertion was successful, false if the storage already contained a vertex with the same id.
   */
  def put(vertex: Vertex[_, _]): Boolean = {
    val intId = getIntForId(vertex.id)
    if (!vertexMap.containsKey(intId)) {
      vertexMap.put(intId, vertex)
      true
    } else
      false
  }

  /**
   * Removes a vertex from the collection and also removes its entires in the toSignal and toCollect collections.
   *
   * @param id the ID of the vertex to remove
   */
  def remove(id: Any) = {
    val vertex = vertexMap.remove(getIntForId(id))
    storage.toSignal.remove(vertex)
  }

  /**
   * Applies the supplied function to each stored vertex
   * 
   * @param f Function to apply to each stored vertex
   */
  def foreach[U](f: Vertex[_, _] => U) {
    val it = vertexMap.values.iterator
    while (it.hasNext) {
      val vertex = it.next
      f(vertex.asInstanceOf[Vertex[_,_]])
    }
  }

  /**
   * Number of vertices held by the collection
   *
   * @return number of vertices
   */
  def size: Long = vertexMap.size

  /**
   * Removes all the vertices from the collection.
   */
  def cleanUp = vertexMap.clear
}

trait TroveStorageBackend extends DefaultStorage {
  override def vertexStoreFactory: VertexStore = new TroveStorage(this)
}