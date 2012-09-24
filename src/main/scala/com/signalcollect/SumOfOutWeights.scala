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
import com.signalcollect._
import scala.reflect.BeanProperty

trait SumOfOutWeights[Id, State] extends AbstractVertex[Id, State] {

  /**
   * @return The sum of the weights of all outgoing edges.
   */
  var sumOfOutWeights: Double = 0

  abstract override def addEdge(e: Edge[_], graphEditor: GraphEditor): Boolean = {
    val added = super.addEdge(e, graphEditor)
    if (added) {
      sumOfOutWeights += e.weight
    }
    added
  }

  abstract override def removeEdge(targetId: Any, graphEditor: GraphEditor): Boolean = {
    var weightToSubtract = 0.0
    val outgoingEdge = outgoingEdges.get(targetId)
    if (outgoingEdge != null) {
      weightToSubtract = outgoingEdge.weight
    }
    val removed = super.removeEdge(targetId, graphEditor)
    if (removed) {
      sumOfOutWeights -= weightToSubtract
    }
    removed
  }

}