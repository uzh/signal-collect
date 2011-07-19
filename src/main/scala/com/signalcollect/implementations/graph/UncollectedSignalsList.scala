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

trait UncollectedSignalsList[IdType, StateType] extends AbstractVertex[IdType, StateType] {

  /** a buffer containing uncollected messages */
  protected val uncollectedMessages: Buffer[Signal[_, _, UpperSignalTypeBound]] = ListBuffer[Signal[_, _, UpperSignalTypeBound]]()
  
  /** traversable uncollected signals */  
  protected def uncollectedSignals: Iterable[UpperSignalTypeBound] = {
    uncollectedMessages map (_.signal)
  }
  
  /** traversable uncollected signals */  
  protected def uncollectedSignals[G](filterClass: Class[G]): Iterable[G] = {
    uncollectedMessages flatMap (message => Filter.bySuperClass(filterClass, message.signal))
  }

  /**
   * This method adds a signal to this {@link Vertex}, which will later be collectible
   * by the {@link #collect} method. This method is going to be called by the SignalCollect framework
   * during its execution (i.e. the {@link Worker} implementations).
   *
   * @param s the signal to add (deliver).
   * @see #collect
   */
  abstract override protected def process(s: Signal[_, _, _]) {
	super.process(s)
    uncollectedMessages += s.asInstanceOf[Signal[_, _, UpperSignalTypeBound]]
  }

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  abstract override def executeCollectOperation(signals: Option[List[Signal[_, _, _]]]) {
	super.executeCollectOperation(signals)
	uncollectedMessages.clear
  }
  
    /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  abstract override def executeCollectOperation {
	super.executeCollectOperation
	uncollectedMessages.clear
  }
  
}