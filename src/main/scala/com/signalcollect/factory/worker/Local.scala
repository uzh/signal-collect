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

package com.signalcollect.factory.worker

import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.implementations.messaging.DefaultMessageBus
import com.signalcollect.interfaces.WorkerFactory
import com.signalcollect.configuration.WorkerConfiguration
import com.signalcollect.implementations.worker.LocalWorker
import com.signalcollect.interfaces.Worker

/**
 *  The local worker factory creates worker instances that work in the local-machine scenario.
 */
object Local extends WorkerFactory {
  def createInstance(workerId: Int,
    workerConfig: WorkerConfiguration,
    numberOfWorkers: Int,
    coordinator: Any,
    loggingLevel: Int): Worker = new LocalWorker(workerId, workerConfig, numberOfWorkers, coordinator, loggingLevel)
}