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

import com.signalcollect.Vertex

/**
 * High level interface to abstract all vertex storage related implementations
 */
trait Storage[@specialized(Int, Long) Id] {
  def vertices: VertexStore[Id]
  def toSignal: VertexStore[Id] //collection of all vertices that need to signal
  def toCollect: VertexStore[Id] //collection of all vertices that need to collect
}

/**
 * Stores vertices and makes them retrievable through their associated id.
 */
trait VertexStore[@specialized(Int, Long) Id] {
  def get(id: Id): Vertex[Id, _]
  def put(vertex: Vertex[Id, _]): Boolean
  def remove(id: Id)
  def isEmpty: Boolean
  def size: Long
  def stream: Stream[Vertex[Id, _]]
  def foreach(f: Vertex[Id, _] => Unit)
  def process(p: Vertex[Id, _] => Unit, numberOfVertices: Option[Int] = None): Int
  def processWithCondition(p: Vertex[Id, _] => Unit, breakCondition: () => Boolean): Int
}

