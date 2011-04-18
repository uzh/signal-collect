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

import signalcollect.implementations.graph._

/**
 * Default [[signalcollect.interfaces.Vertex]] implementation.
 *
 * @param id unique vertex id
 * @param initialState initial state of this vertex
 *
 * Vertices are the main processing units in a Signal/Collect algorithm.
 * The only method that has to be implemented is the abstract collect function.
 * The collect function uses the received signals to calculate the new state.
 */
abstract class DefaultVertex[IdType, StateType](
  val id: IdType,
  initialState: StateType)
  extends AbstractVertex[IdType, StateType]
  with UncollectedSignalsList[IdType, StateType]
  with MostRecentSignalMap[IdType, StateType]
  with IncomingEdgeCount[IdType, StateType]
  with SumOfOutWeights[IdType, StateType] {

  /** vertex state is initialized to initialState */
  var state = initialState

}