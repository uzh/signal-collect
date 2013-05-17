/*
 *  @author Philip Stutz
 *
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.factory.workerapi

import com.signalcollect.coordinator.DefaultWorkerApi
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.interfaces.WorkerApi
import com.signalcollect.interfaces.WorkerApiFactory

object DefaultWorkerApiFactory extends WorkerApiFactory {
  override def createInstance[Id, Signal](
    workerProxies: Array[WorkerApi[Id, Signal]],
    mapper: VertexToWorkerMapper[Id]): WorkerApi[Id, Signal] = {
    new DefaultWorkerApi(workerProxies, mapper)
  }
  override def toString = "DefaultWorkerApiFactory"
}