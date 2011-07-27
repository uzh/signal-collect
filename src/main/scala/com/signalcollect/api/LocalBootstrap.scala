/*
 *  @author Francisco de Freitas
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

package com.signalcollect.api

import com.signalcollect.interfaces._
import com.signalcollect.configuration._
import com.signalcollect.implementations.coordinator._
import com.signalcollect.implementations.logging._
import com.signalcollect.api.factory._

/**
 * Booting sequence for running Signal Collect locally
 */
class LocalBootstrap(val config: Configuration) extends Bootstrap {

  protected def createLogger: MessageRecipient[LogMessage] = new DefaultLogger

  protected def createWorkers(workerApi: WorkerApi) {

    for (workerId <- 0 until config.numberOfWorkers) {

      config.workerConfiguration.workerFactory match {
        case worker.Local => workerApi.createWorker(workerId).asInstanceOf[Worker].initialize
        case _            => throw new Exception("Only local workers supported by this Bootstrap")
      }
    }

  }

  protected def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): DefaultComputeGraph = {
    new DefaultComputeGraph(config, workerApi, coordinator)
  }

  def shutdown {
    println("shutdown")
  }

}