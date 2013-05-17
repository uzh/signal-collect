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
import com.signalcollect.worker.ThrottlingBulkScheduler

/**
 *  The local worker factory creates worker instances that work in the local-machine scenario.
 */
object LocalWorker extends WorkerFactory {
  def createInstance[Id: ClassTag, Signal: ClassTag](
    workerId: Int,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    config: GraphConfiguration): WorkerActor[Id, Signal] = {
    new AkkaWorker[Id, Signal](
      workerId,
      numberOfWorkers,
      numberOfNodes,
      config.messageBusFactory,
      config.storageFactory,
      config.heartbeatIntervalInMilliseconds)
  }
  override def toString = "LocalWorkerFactory"
}

/**
 *  The distributed worker factory creates worker instances that support throttling and bulk-sending.
 */
object DistributedWorker extends WorkerFactory {
  def createInstance[Id: ClassTag, Signal: ClassTag](
    workerId: Int,
    numberOfWorkers: Int,
    numberOfNodes: Int,
    config: GraphConfiguration): WorkerActor[Id, Signal] = {
    new AkkaWorker[Id, Signal](
      workerId,
      numberOfWorkers,
      numberOfNodes,
      config.messageBusFactory,
      config.storageFactory,
      config.heartbeatIntervalInMilliseconds) with ThrottlingBulkScheduler[Id, Signal]
  }
  override def toString = "DistributedWorkerFactory"
}