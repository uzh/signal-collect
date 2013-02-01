/*
 *  @author Silvan Troxler
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

package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.interfaces.SignalMessage
import scala.collection.mutable.IndexedSeq
import scala.collection.mutable.Set


/**
 * SubType connection from one Type to another Type
 */
class SubType(t: Any) extends OnlySignalOnChangeEdge(t) {
  type Source = Type
  
  /**
   * Signaling the current state of a Type combined with its own ID
   */
  def signal = {
    source.state + (source.id.toString.toInt)
  }
}


/**
 * Type which is based on a DataFlowVertex
 */
class Type(vertexId: Any, initialState: Set[Int] = Set()) extends DataFlowVertex(vertexId, initialState) {
  type Signal = Set[Int]
  
  /**
   * Combination of the current state of a vertex and the collected signal
   */
  def collect(signal: Set[Int]): Set[Int] = {
    state ++ (signal)
  }

}

/**
 * Builds a tree consisting of several Types and SubType-connections, then execute the computation on that graph
 */
object TransitiveClosure extends App {
  
  /* Visual Representation of the graph to be created:
   *      1
   *   2     5
   *  3 4   6 7
   */
  
  val graph = GraphBuilder.build
  graph.addVertex(new Type(1, Set(0)))
  graph.addVertex(new Type(2))
  graph.addVertex(new Type(3))
  graph.addVertex(new Type(4))
  graph.addVertex(new Type(5))
  graph.addVertex(new Type(6))
  graph.addVertex(new Type(7))
  graph.addEdge(1, new SubType(2))
  graph.addEdge(1, new SubType(5))
  graph.addEdge(2, new SubType(3))
  graph.addEdge(2, new SubType(4))
  graph.addEdge(5, new SubType(6))
  graph.addEdge(5, new SubType(7))
  val stats = graph.execute
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
