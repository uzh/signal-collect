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

package com.signalcollect.factory.worker

import scala.reflect.ClassTag
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.interfaces.WorkerActor
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.worker.AkkaWorker
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.interfaces.SchedulerFactory

/**
 *  The default Akka worker implementation.
 */
object DefaultAkkaWorker extends WorkerFactory {
  override def createInstance[Id: ClassTag, Signal: ClassTag](
    workerId: Int,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    messageBusFactory: MessageBusFactory,
    mapperFactory: MapperFactory,
    storageFactory: StorageFactory,
    schedulerFactory: SchedulerFactory,
    heartbeatIntervalInMilliseconds: Int,
    eagerIdleDetection: Boolean): WorkerActor[Id, Signal] = {
    new AkkaWorker[Id, Signal](
      workerId,
      numberOfWorkers,
      numberOfNodes,
      messageBusFactory,
      mapperFactory,
      storageFactory,
      schedulerFactory,
      heartbeatIntervalInMilliseconds,
      eagerIdleDetection)
  }
  override def toString: String = "DefaultAkkaWorker"
}
