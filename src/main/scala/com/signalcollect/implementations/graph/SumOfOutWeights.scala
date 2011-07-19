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

package com.signalcollect.implementations.graph

import com.signalcollect.interfaces.Edge

trait SumOfOutWeights[IdType, StateType] extends AbstractVertex[IdType, StateType] {

  /** @return the sum of the weights of all outgoing edges of this {@link FrameworkVertex}. */
  var sumOfOutWeights: Double = 0

  abstract override def addOutgoingEdge(e: Edge[_, _]): Boolean = {
    val added = super.addOutgoingEdge(e)
    if (added) {
      sumOfOutWeights = sumOfOutWeights + e.weight
    }
    added
  }

  abstract override def removeOutgoingEdge(edgeId: (Any, Any, String)): Boolean = {
    var weightToSubtract = 0.0
    val castEdgeId = edgeId.asInstanceOf[(IdType, Any, String)]
    val optionalOutgoinEdge = outgoingEdges.get(castEdgeId)
    if (optionalOutgoinEdge.isDefined) {
      weightToSubtract = optionalOutgoinEdge.get.weight
    }
    val removed = super.removeOutgoingEdge(edgeId)
    if (removed) {
      sumOfOutWeights -= weightToSubtract
    }
    removed
  }

}