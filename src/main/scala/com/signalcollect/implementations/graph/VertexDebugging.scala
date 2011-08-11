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
import scala.collection.parallel.immutable.ParHashMap
import com.signalcollect.interfaces._
import scala.collection.GenMap

trait VertexDebugging extends AbstractVertex { 
    
  protected val signalSourceDebuggingMap = HashMap[EdgeId[_, _], Any]() // key: signal source id, value: signal

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  abstract override def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any]) {
    val castS = signals.asInstanceOf[Iterable[SignalMessage[_, _, Any]]]
    castS foreach { signalMessage =>
      signalSourceDebuggingMap.put(signalMessage.edgeId, signalMessage.signal)
    }
    super.executeCollectOperation(signals, messageBus)
  }

  /**
   * Returns the ids of all vertices from which this vertex has an incoming edge, optional.
   */
  abstract override def getVertexIdsOfPredecessors: Option[Iterable[_]] = {
    Some(signalSourceDebuggingMap.keys map (_.sourceId))
  }
  
  /**
   * Returns the most recent signal sent via the edge with the id @edgeId. None if this function is not
   * supported or if there is no such signal.
   */
  abstract override def getMostRecentSignal(id: EdgeId[_, _]): Option[_] = {
    signalSourceDebuggingMap.get(id.asInstanceOf[EdgeId[_, _]])
  }
  
}