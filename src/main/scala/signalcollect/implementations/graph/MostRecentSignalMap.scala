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

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import signalcollect.interfaces.Signal

trait MostRecentSignalMap[IdType, StateType] extends AbstractVertex[IdType, StateType] {

  protected val mostRecentSignalMap: Map[Any, UpperSignalTypeBound] = HashMap[Any, UpperSignalTypeBound]() // key: signal source id, value: signal

  protected def mostRecentSignals: Iterable[UpperSignalTypeBound] = mostRecentSignalMap.values
  
  protected def signals[G <: Any](implicit m: Manifest[G]): Traversable[G] = new Traversable[G] {
    def foreach[U](f: G => U) = {
      mostRecentSignalMap.valuesIterator foreach { x => try { f(x.asInstanceOf[G]) } catch { case _ => } } // not nice, but isAssignableFrom is slow and has nasty issues with boxed/unboxed
    }
  }

  protected def mostRecentSignalFrom[G <: Any](id: Any): Option[G] = {
    mostRecentSignalMap.get(id) match {
    	case Some(x) => try { Some(x.asInstanceOf[G]) } catch { case _ => None }
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