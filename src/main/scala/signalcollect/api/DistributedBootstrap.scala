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

package signalcollect.api

import signalcollect.interfaces._
import signalcollect.configuration._
import signalcollect.configuration.bootstrap._
import signalcollect.implementations.coordinator._
import signalcollect.implementations.logging._

/**
 * The bootstrap sequence for initializing the distributed infrastructure 
 */
class DistributedBootstrap(val config: Configuration) extends Bootstrap {

  override def boot: ComputeGraph = {
    deploy
    super.boot
  }

  def deploy {
    // start managers
  }
  
  protected def createOptionalLogger: Option[Logger] = {
    
    var logger: Option[Logger] = None
    
    // start logger based on config TODO: add correct one to be instantiated
    if (config.optionalLogger) {
      logger = Some(new DefaultLogger())
    }
    
    logger
  }

  def createWorkers(workerApi: WorkerApi) {

    val workersPerNode = config.bootstrapConfiguration.nodeProvisioning.workerPorts

    // TODO: correctly start remote workers
    // TODO: add supervising hierarchical layer

  }

  def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): DefaultComputeGraph = {
    null
  }

  def shutdown {
    // signal managers
    println("shutdown")
  }

}