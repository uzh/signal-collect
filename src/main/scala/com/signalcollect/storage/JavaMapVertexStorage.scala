/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.storage

import com.signalcollect.interfaces.Storage
import com.signalcollect.interfaces.VertexStore

class JavaMapVertexStorage[Id] extends Storage[Id] {
  val vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore[Id] = new JavaVertexMap[Id]

  val toCollect = vertexSignalFactory //holds all signals that are not collected yet
  protected def vertexSignalFactory = new JavaVertexMap[Id]
  val toSignal = vertexSetFactory //holds all vertex ids that need to signal
  protected def vertexSetFactory = new JavaVertexMap[Id]

}