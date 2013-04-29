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
  def outgoingEdges: collection.mutable.Map[Any, Edge[_]] 

}
