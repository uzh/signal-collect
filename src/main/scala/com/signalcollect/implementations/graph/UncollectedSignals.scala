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

import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.Signal
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer
import com.signalcollect.interfaces.MessageBus

trait UncollectedSignalsList[IdType, StateType] extends AbstractVertex[IdType, StateType] {

  /** a buffer containing uncollected messages */
  protected var uncollectedMessages: Iterable[Signal[_, _, UpperSignalTypeBound]] = _
  
  /** traversable uncollected signals */  
  protected def uncollectedSignals: Iterable[UpperSignalTypeBound] = {
    uncollectedMessages map (_.signal)
  }
  
  /** traversable uncollected signals */  
  protected def uncollectedSignals[G](filterClass: Class[G]): Iterable[G] = {
    uncollectedMessages flatMap (message => Filter.bySuperClass(filterClass, message.signal))
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  abstract override def executeCollectOperation(signals: Iterable[Signal[_, _, _]], messageBus: MessageBus[Any]) {
    uncollectedMessages = signals.asInstanceOf[Iterable[Signal[_, _, UpperSignalTypeBound]]]
	super.executeCollectOperation(signals, messageBus)
  }
  
}