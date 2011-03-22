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

package signalcollect.implementations.graph

import signalcollect.interfaces.Edge
trait SumOfOutWeights[IdType, StateType] extends AbstractVertex[IdType, StateType] {
	
  /** @return the sum of the weights of all outgoing edges of this {@link FrameworkVertex}. */
  var sumOfOutWeights: Double = 0
	
  protected abstract override def processNewOutgoingEdge(e: Edge[IdType, _]) {
      super.processNewOutgoingEdge(e)
      sumOfOutWeights = sumOfOutWeights + e.weight
  }
  
  protected abstract override def processRemoveOutgoingEdge(e: Edge[IdType, _]) {
      super.processNewOutgoingEdge(e)
      sumOfOutWeights = sumOfOutWeights - e.weight
  }
  
}