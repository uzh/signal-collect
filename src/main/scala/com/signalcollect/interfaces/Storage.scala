/*
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
 */

package com.signalcollect.interfaces

import com.signalcollect._

/**
 * High level interface to abstract all vertex storage related implementations
 */
abstract class Storage {
  def vertices: VertexStore
  def toSignal: VertexIdSet //collection of all vertices that need to signal
  def toCollect: VertexSignalBuffer // collection of all vertices that need to collect
  def cleanUp
  def serializer: Serializer
}

/**
 * Stores vertices and makes them retrievable through their associated id.
 */
trait VertexStore {
  def get(id: Any): Vertex
  def getAll(condition: Vertex => Boolean): List[Vertex]
  def put(vertex: Vertex): Boolean
  def remove(id: Any)
  def updateStateOfVertex(vertex: Vertex)
  def size: Long
  def foreach[U](f: Vertex => U)
  def cleanUp
}

/**
 * Allows storing a set of id and iterating through them
 */
trait VertexIdSet {
  def add(vertexId: Any)
  def remove(vertexId: Any)
  def size: Int
  def isEmpty: Boolean
  def foreach[U](f: Any => U, removeAfterProcessing: Boolean)
  def applyToNext[U](f: (Any) => U, removeAfterProcessing: Boolean)
  def updateStateOfVertex(vertex: Vertex)
  def cleanUp
}

/**
 * Allows storing a collection of signals and iterating through them
 */
trait VertexSignalBuffer {
  def addSignal(signal: SignalMessage[_, _, _])
  def addVertex(vertexId: Any)
  def remove(vertexId: Any)
  def size: Int
  def isEmpty: Boolean
  def foreach[U](f: (Any, Iterable[SignalMessage[_, _, _]]) => U, removeAfterProcessing: Boolean, breakCondition: () => Boolean = () => false): Boolean
  def cleanUp
}

/**
 * Defines all functionality needed for serialization/deserialization
 */
trait Serializer {
  def write[A](inputObject: A): Array[Byte]
  def read[A](buffer: Array[Byte]): A
}
