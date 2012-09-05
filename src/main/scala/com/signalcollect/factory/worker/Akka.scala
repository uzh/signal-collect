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

import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.messaging.DefaultMessageBus
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.interfaces.Worker
import com.signalcollect.worker.AkkaWorker
import com.signalcollect.configuration.GraphConfiguration
import akka.actor.ActorRef
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.worker.ImmediateCollectScheduler

/**
 *  The local worker factory creates worker instances that work in the local-machine scenario.
 */
object Akka extends WorkerFactory {
  def createInstance(
    workerId: Int,
    numberOfWorkers: Int,
    messageBusFactory: MessageBusFactory,
    storageFactory: StorageFactory,
    statusUpdateIntervalInMillis: Option[Long],
    loggingLevel: Int): Worker = {
    new AkkaWorker(workerId, numberOfWorkers, messageBusFactory, storageFactory, statusUpdateIntervalInMillis, loggingLevel)
  }
}

object CollectFirstAkka extends WorkerFactory {
  def createInstance(
    workerId: Int,
    numberOfWorkers: Int,
    messageBusFactory: MessageBusFactory,
    storageFactory: StorageFactory,
    statusUpdateIntervalInMillis: Option[Long],
    loggingLevel: Int): Worker = {
    new AkkaWorker(workerId, numberOfWorkers, messageBusFactory, storageFactory, statusUpdateIntervalInMillis, loggingLevel) with ImmediateCollectScheduler
  }
}