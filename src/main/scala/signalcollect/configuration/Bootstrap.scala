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

package signalcollect.configuration

import signalcollect.api._
import signalcollect.interfaces._
import signalcollect.implementations.coordinator._
import signalcollect.configuration._

/**
 * Bootstrap generalization for starting Signal Collect infrastructure
 */
trait Bootstrap {

  // message bus for the bootstrap to send messages to the logger
  protected val messageBus: MessageBus[Any] = config.workerConfiguration.messageBusFactory.createInstance(config.numberOfWorkers, null)

  // the compute graph, the heart of signal collect
  protected var computeGraph: ComputeGraph = _

  // the configuration for the system
  def config: Configuration

  /**
   * Creates the logger system based on the type of architecture selected.
   * This in turn helps the system to have the right logger for usage.
   */
  protected def createLogger: MessageRecipient[LogMessage]

  /**
   * The correct execution for the startup of signal collect's infrastructure
   * This method is called to return the right Compute Graph based on the configuration given
   */
  def boot: ComputeGraph = {

    // create optional logger
    var logger = createLogger

    val workerApi = new WorkerApi(config, logger)

    createWorkers(workerApi)

    workerApi.initialize

    val coordinator = new Coordinator(workerApi, config)

    computeGraph = createComputeGraph(workerApi, coordinator)

    computeGraph
  }

  /**
   * Creation of workers for the worker api. Local and distributed have different initialization setups
   */
  protected def createWorkers(workerApi: WorkerApi)

  /**
   * Gets the compute graph instance properly configured
   */
  protected def createComputeGraph(workerApi: WorkerApi, coordinator: Coordinator): ComputeGraph

  /**
   * FIXME: This start can be replaced by the execute function on compute graph but 
   * I have to see if I need to do anything else on the distributed case.
   */
  def start {
    computeGraph.execute
  }

  /**
   * Shuts down the distributed infrastructure 
   */
  def shutdown

}
