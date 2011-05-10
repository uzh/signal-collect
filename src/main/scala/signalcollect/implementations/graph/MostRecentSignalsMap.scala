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

import util.collections.Filter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import signalcollect.interfaces.Signal

trait MostRecentSignalsMap[IdType, StateType] extends AbstractVertex[IdType, StateType] {
  
  protected val mostRecentSignalMap: Map[Any, UpperSignalTypeBound] = HashMap[Any, UpperSignalTypeBound]() // key: signal source id, value: signal

  protected def mostRecentSignals: Iterable[UpperSignalTypeBound] = mostRecentSignalMap.values

  protected def signals[G](filterClass: Class[G]): Iterable[G] = {
    mostRecentSignalMap.values flatMap (value => Filter.byClass(filterClass, value))
  }

  protected def mostRecentSignalFrom[G](signalClass: Class[G], id: Any): Option[G] = {
    mostRecentSignalMap.get(id) match {
      case Some(x) => Filter.byClass(signalClass, x)
      case other => None
    }
  }

  /**
   * This method adds a signal to this {@link Vertex}, which will later be collectible
   * by the {@link #collect} method. This method is going to be called by the SignalCollect framework
   * during its execution (i.e. the {@link Worker} implementations).
   *
   * @param s the signal to add (deliver).
   * @see #collect
   */
  abstract override protected def process(s: Any) {
    super.process(s)
    val castS = s.asInstanceOf[Signal[_, _, UpperSignalTypeBound]]
    mostRecentSignalMap.put(castS.sourceId, castS.signal)
  }

}