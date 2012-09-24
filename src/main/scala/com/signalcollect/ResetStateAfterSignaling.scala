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

import com.signalcollect._
import com.signalcollect.interfaces.MessageBus

trait ResetStateAfterSignaling[Id, State] extends AbstractVertex[Id, State] {
  
  def resetState: State

  /**
   * Delegates to superclass and resets the state to the initial state after signaling.
   */
  abstract override def executeSignalOperation(graphEditor: GraphEditor) {
    super.executeSignalOperation(graphEditor)
    setState(resetState)
  }

}