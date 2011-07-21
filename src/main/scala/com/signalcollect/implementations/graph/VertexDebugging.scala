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

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import com.signalcollect.interfaces.Signal
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.Logging

trait VertexDebugging[IdType, StateType] extends AbstractVertex[IdType, StateType] with Logging {

  protected var messageBus: MessageBus[Any] = _

  abstract override def afterInitialization(mb: MessageBus[Any]) {
    super.afterInitialization(mb)
    messageBus = mb
  }
  
  abstract override def executeSignalOperation(mb: MessageBus[Any]) {
    super.executeSignalOperation(mb: MessageBus[Any])
    debug("Signaling")
  }
  
  abstract override def executeCollectOperation(signalList: List[Signal[_, _, _]], mb: MessageBus[Any]) {
    debug("State before collect: " + state + ", Signal list for collection: " + signalList)
    super.executeCollectOperation(signalList, mb)
    debug("State after collect: " + state)
  }
	
}