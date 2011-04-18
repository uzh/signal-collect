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

package signalcollect.api

import signalcollect.interfaces._
import signalcollect.implementations.graph.AbstractEdge

/**
 * Companion object that provides a simplified constructor
 */
object StateForwarderEdge {
	def apply(sId: Any, tId: Any) = new StateForwarderEdge(sId, tId)
}

/**
 * [[signalcollect.interfaces.Edge]] implementation that sends the state
 * of the source vertex as the signal.
 *
 * @param sourceId id of this edge's source vertex
 * @param targetId id of this edges's target vertex
 *
 * See [[signalcollect.api.DefaultEdge]] for more information about edges
 * in general.
 */
class StateForwarderEdge(sourceId: Any, targetId: Any) extends DefaultEdge(sourceId, targetId) {
  def signal = source.state
}