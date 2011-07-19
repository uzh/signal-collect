/*
 *  @author Philip Stutz
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
 *  
 */

package com.signalcollect.javaapi

import com.signalcollect.interfaces.Vertex
import com.signalcollect.implementations.graph.AbstractEdge

/**
 * Default [[com.signalcollect.interfaces.Edge]] implementation.
 *
 * @param sourceId id of this edge's source vertex
 * @param targetId id of this edges's target vertex
 *
 * Edges send signals from the source vertex to the target vertex.
 * The only method that has to be implemented is the abstract signal function.
 * The signal function usually uses the state of the source vertex
 * to calculate the signal sent to the target vertex.
 */
class DefaultEdgeJ[SourceIdType, TargetIdType](
  val sourceId: SourceIdType,
  val targetId: TargetIdType)
  extends AbstractEdge[SourceIdType, TargetIdType] {
  
  def signal(sourceVertex: SourceVertexType): Object = null.asInstanceOf[Object]

}


