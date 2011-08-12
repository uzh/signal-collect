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

import com.signalcollect.implementations.graph._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import com.signalcollect.interfaces._
import com.signalcollect.util.collections.Filter
import com.signalcollect.interfaces.MessageBus
import scala.reflect.BeanProperty

abstract class CommandJ extends Function1[Vertex, Unit] {
  def apply(v: Vertex) = f(v); Unit
  def f(v: Vertex)
}