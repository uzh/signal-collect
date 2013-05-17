/*
 *  @author Philip Stutz
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
 *
 */

package com.signalcollect.interfaces

import scala.reflect.ClassTag

import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.factory.workerapi.DefaultWorkerApiFactory

trait Factory extends Serializable

trait WorkerFactory extends Factory {
  def createInstance[Id: ClassTag, Signal: ClassTag](
    workerId: Int,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    config: GraphConfiguration): WorkerActor[Id, Signal]
}

trait MessageBusFactory extends Factory {
  def createInstance[Id: ClassTag, Signal: ClassTag](
    numberOfWorkers: Int,
    numberOfNodes: Int,
    sendCountIncrementorForRequests: MessageBus[_, _] => Unit,
    workerApiFactory: WorkerApiFactory = DefaultWorkerApiFactory): MessageBus[Id, Signal]
}

trait StorageFactory extends Factory {
  def createInstance[Id]: Storage[Id]
}

trait WorkerApiFactory extends Factory {
  def createInstance[Id, Signal](
    workerProxies: Array[WorkerApi[Id, Signal]],
    mapper: VertexToWorkerMapper[Id]): WorkerApi[Id, Signal]
}