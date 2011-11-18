/*
 *  @author Philip Stutz
 *  
 *  Copyright 2011 University of Zurich
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

package com.signalcollect.javaapi

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.MessageBus
import scala.reflect.BeanProperty
import scala.collection.JavaConversions
import com.signalcollect.implementations.graph.AbstractVertex
import com.signalcollect.implementations.graph.SumOfOutWeights
import com.signalcollect.implementations.graph.VertexGraphEditor
import com.signalcollect.implementations.graph.ResetStateAfterSignaling
import com.signalcollect.interfaces.SignalMessage

/**
 *  A version of the abstract vertex that serves as the foundation for the Java API vertices.
 *  It translates type parameters to Scala type members, which are unavailable in Java.
 *
 *  @note This class is concrete, because this avoids some of the troubles when extending Scala
 *  traits or abstract classes with a Java class.
 *
 *  @note The bean property annotation tells the compiler to generate getters and setters, which makes fields
 *  accessible from Java.
 */

class JavaVertex[IdTypeParameter, StateTypeParameter, SignalTypeParameter](
  @BeanProperty val id: IdTypeParameter,
  @BeanProperty var state: StateTypeParameter)
  extends AbstractVertex with SumOfOutWeights with VertexGraphEditor {

  type Id = IdTypeParameter
  type State = StateTypeParameter
  type Signal = SignalTypeParameter

  def executeCollectOperation(signals: Iterable[SignalMessage[_, _, _]], messageBus: MessageBus[Any]) {
  }

}

class JavaVertexWithResetStateAfterSignaling[@specialized IdTypeParameter, @specialized StateTypeParameter, @specialized SignalTypeParameter](
  id: IdTypeParameter, initialState: StateTypeParameter,
  val resetState: StateTypeParameter)
  extends JavaVertex[IdTypeParameter, StateTypeParameter, SignalTypeParameter](
    id,
    initialState)
  with ResetStateAfterSignaling
