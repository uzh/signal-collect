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

import java.util.Set
import com.signalcollect.interfaces._
import com.signalcollect.serialization._
import java.util.Set

/**
 * Default configuration for storing vertices and the toSignal and toCollect collections
 * Uses in-memory implementations for all collections.
 */
class DefaultStorage extends Storage {
  
  var vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore = new InMemoryStorage(this)
  
  var toCollect = vertexSignalFactory //holds all signals that are not collected yet
  protected def vertexSignalFactory: VertexSignalBuffer = new InMemoryVertexSignalBuffer 
  var toSignal = vertexSetFactory //holds all vertex ids that need to signal
  protected def vertexSetFactory: VertexIdSet = new InMemoryVertexIdSet(this)
  
  def serializer: Serializer = DefaultSerializer
  
  def cleanUp {
    vertexStoreFactory.cleanUp
    toCollect.cleanUp
    toSignal.cleanUp
  }
}