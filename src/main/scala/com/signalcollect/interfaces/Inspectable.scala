/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

package com.signalcollect.interfaces

import com.signalcollect.{ Vertex, Edge }

/**
 *  Trait that can be mixed into a vertex implementation in
 *  order to make a vertex accessible to graph inspector visualization plug-in.
 */
trait Inspectable[Id, State] extends Vertex[Id, State] {

  /**
   * Returns the ids of the target vertices of outgoing edges of the vertex.
   */
  def getTargetIdsOfOutgoingEdges: Traversable[_]

  /**
   * Returns a Traversable of the edges this vertex is connected to.
   */ 
  def edges: Traversable[Edge[_]]

  /**
   * The expose function can provide additional information about the vertex.
   * By default, it returns an empty map, but it can be overridden to return
   * any kind of Map[String,Any]. The Map will be serialized to json
   * recursively and can be viewed in the console when enabling the "expose
   * vertex details on click" option. 
   * @return a string-keyed map of details about the node.
   */
  def expose: Map[String,Any] = Map[String,Any]()

}
