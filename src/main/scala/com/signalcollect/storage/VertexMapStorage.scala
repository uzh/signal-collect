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

package com.signalcollect.storage

import com.signalcollect.interfaces.Storage
import com.signalcollect.interfaces.VertexStore

object StorageDefaultValues {
  final val defaultInitialSize = 32768
  final val defaultRehashFraction = 0.5f
  final val defaultShrinkFraction = 0.05f
  final val defaultVertexMapRehashFraction = 0.8f
  final val minShrinkSize = 16384 // Will not shrink if the map capacity is at or below this.
  final val defaultTemporaryMapRehashFraction = 0.5f
  final val defaultToSignalInitialSize = 1024
}

/**
 *  Storage backed by a custom-tailored open hash map implementation for vertices.
 */
class VertexMapStorage[Id, Signal] extends Storage[Id, Signal] {
  import StorageDefaultValues._

  val vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore[Id, Signal] = new VertexMap[Id, Signal](
    initialSize = defaultInitialSize, rehashFraction = defaultVertexMapRehashFraction)

  val toCollect = vertexSignalFactory //holds all signals that are not collected yet
  protected def vertexSignalFactory = new VertexMap[Id, Signal](
    initialSize = defaultInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = vertexSetFactory //holds all vertex ids that need to signal
  protected def vertexSetFactory = new VertexMap[Id, Signal](
    initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)

}
