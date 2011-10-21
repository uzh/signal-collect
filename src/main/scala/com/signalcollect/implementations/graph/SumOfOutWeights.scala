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

import com.signalcollect.interfaces._
import com.signalcollect._
import scala.reflect.BeanProperty

trait SumOfOutWeights extends AbstractVertex {  

  /** @return the sum of the weights of all outgoing edges of this {@link FrameworkVertex}. */
  @BeanProperty var sumOfOutWeights: Double = 0

  abstract override def addOutgoingEdge(e: Edge): Boolean = {
    val added = super.addOutgoingEdge(e)
    if (added) {
      sumOfOutWeights = sumOfOutWeights + e.weight
    }
    added
  }

  abstract override def removeOutgoingEdge(edgeId: EdgeId[_, _]): Boolean = {
    var weightToSubtract = 0.0
    val castEdgeId = edgeId.asInstanceOf[EdgeId[Id, _]]
    val outgoinEdge = outgoingEdges.get(castEdgeId)
    if (outgoinEdge != null) {
      weightToSubtract = outgoinEdge.weight
    }
    val removed = super.removeOutgoingEdge(edgeId)
    if (removed) {
      sumOfOutWeights -= weightToSubtract
    }
    removed
  }

}