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

/**
 * High level interface to abstract all vertex storage related implementations
 */
abstract class Storage {
  def vertices: VertexStore
  def toSignal: VertexIdSet //collection of all vertices that need to signal
  def toCollect: VertexSignalBuffer // collection of all vertices that need to collect
  def cleanUp
}

/**
 * Stores vertices and makes them retrievable through their associated id.
 */
trait VertexStore {
  def get(id: Any): Vertex[_, _]
  def put(vertex: Vertex[_, _]): Boolean
  def remove(id: Any)
  def updateStateOfVertex(vertex: Vertex[_, _])
  def size: Long
  def foreach[U](f: (Vertex[_, _]) => U)
  def cleanUp
}

/**
 * Allows storing a set of id and iterating through them
 */
trait VertexIdSet {
  def add(vertexId: Any)
  def remove(vertexId: Any)
  def clear
  def size: Int
  def isEmpty: Boolean
  /* This *removes* the ids from the set! */
  def foreach[U](f: Any => U) // vertex id
  /* This *removes* the ids from the set! */
  def foreachWithSnapshot[U](f: Any => U, breakConditionReached: () => Boolean): Boolean // vertex id
  def cleanUp
}

/**
 * Allows storing a set of id and iterating through them
 */
trait VertexSignalBuffer {
  def addSignal(signal: Signal[_, _, _])
  def addVertex(vertexId: Any)
  def remove(vertexId: Any)
  def clear
  def size: Int
  def isEmpty: Boolean
  /* This does *not* remove the ids from the set! */
  def foreach[U](f: (Any, List[Signal[_, _, _]]) => U) // vertex id, buffered signals for id
  /* This does *not* remove the ids from the set! */
  def foreachWithSnapshot[U](f: (Any, List[Signal[_, _, _]]) => U, breakConditionReached: () => Boolean): Boolean // vertex id, buffered signals for id
  def cleanUp
}

/**
 * Defines all functionality needed for serialization/deserialization
 */
trait Serializer {
  def write[A](inputObject: A): Array[Byte]
  def read[A](buffer: Array[Byte]): A
}
