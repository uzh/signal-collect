/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect

import com.signalcollect.interfaces._

/**
 *  StateForwarderEdge is an edge implementation that signals
 *  the state of its source vertex.
 *
 *  @param sourceId id of this edge's source vertex
 *  @param targetId id of this edges's target vertex
 *  @param description an additional description of this edge that would allow to tell apart multiple edges between the source and the target vertex
 */
class StateForwarderEdge[SourceIdType, TargetIdType](
  sourceId: SourceIdType,
  targetId: TargetIdType,
  description: String = getClass.getSimpleName)
  extends DefaultEdge(sourceId, targetId, description) {

  def this(sourceId: SourceIdType, targetId: TargetIdType) = this(sourceId, targetId, "")
  
  def signal(sourceVertex: SourceVertex) = sourceVertex.state.asInstanceOf[Signal]

}