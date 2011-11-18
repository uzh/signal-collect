/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
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

import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.Vertex
import java.util.Map
import java.util.HashMap

/**
 * Aggregates all the ids and states and returns them as a java compatible map.
 */
class IdStateJavaAggregator[IdType, StateType] extends AggregationOperation[Map[IdType, StateType]] {
  val neutralElement = new HashMap[IdType, StateType]()
  def extract(v: Vertex): Map[IdType, StateType] = {
    try {
      val map = new HashMap[IdType, StateType]()
      map.put(v.id.asInstanceOf[IdType], v.state.asInstanceOf[StateType])
      map
    }
  }
  def aggregate(a: Map[IdType, StateType], b: Map[IdType, StateType]): Map[IdType, StateType] = { 
    a.putAll(b)
    a
  }
}