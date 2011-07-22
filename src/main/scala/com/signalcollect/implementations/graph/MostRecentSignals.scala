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
import com.signalcollect.util.collections.Filter;
import com.signalcollect.interfaces.MessageBus

trait MostRecentSignalsMap[IdType, StateType] extends AbstractVertex[IdType, StateType] {
  
  protected val mostRecentSignalMap: Map[Any, UpperSignalTypeBound] = HashMap[Any, UpperSignalTypeBound]() // key: signal source id, value: signal

  protected def mostRecentSignals: Iterable[UpperSignalTypeBound] = mostRecentSignalMap.values

  protected def signals[G](filterClass: Class[G]): Iterable[G] = {
    mostRecentSignalMap.values flatMap (value => Filter.bySuperClass(filterClass, value))
  }

  protected def mostRecentSignalFrom[G](signalClass: Class[G], id: Any): Option[G] = {
    mostRecentSignalMap.get(id) match {
      case Some(x) => Filter.bySuperClass(signalClass, x)
      case other => None
    }
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  abstract override def executeCollectOperation(signals: Iterable[Signal[_, _, _]], messageBus: MessageBus[Any]) {
    val castS = signals.asInstanceOf[Traversable[Signal[_, _, UpperSignalTypeBound]]]
    castS foreach { signal =>
    	mostRecentSignalMap.put(signal.sourceId, signal.signal)      
    }
	super.executeCollectOperation(signals, messageBus)
  }
  
  
}