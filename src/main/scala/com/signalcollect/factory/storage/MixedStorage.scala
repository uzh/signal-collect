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
 *
 */

package com.signalcollect.factory.storage

import com.signalcollect.interfaces.Storage
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.storage.MixedVertexStorage

/**
 *  Storage backed by a mix between a custom-tailored open hash map implementation for vertices
 *  and the default Java HashMap for toSignal/toCollect.
 */
class MixedStorage[@specialized(Int, Long) Id, Signal] extends StorageFactory[Id, Signal] {
  def createInstance: Storage[Id, Signal] = new MixedVertexStorage[Id, Signal]
  override def toString = "MixedStorage"
}
