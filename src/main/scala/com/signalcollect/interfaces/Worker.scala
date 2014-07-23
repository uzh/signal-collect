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

package com.signalcollect.interfaces

import com.signalcollect.Vertex

trait Worker[Id, Signal]
  extends WorkerApi[Id, Signal]
  with MessageRecipientRegistry {
  def vertexStore: Storage[Id, Signal]
  def scheduler: Scheduler[Id, Signal]
  var messageBusFlushed: Boolean
  def executeCollectOperationOfVertex(vertex: Vertex[Id, _, Id, Signal], addToSignal: Boolean = true)
  def executeSignalOperationOfVertex(vertex: Vertex[Id, _, Id, Signal])
  def signalThreshold: Double
}
