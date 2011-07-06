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

package signalcollect.configuration.bootstrap

import signalcollect.configuration._
import signalcollect.configuration.provisioning._

/**
 * Configuration parameters for bootstrapping
 * Mainly used by the distributed case
 */
trait BootstrapConfiguration {
  def executionArchitecture: ExecutionArchitecture
  def numberOfNodes: Int
  def nodesAddress: Vector[String]
  def coordinatorAddress: String
  def nodeProvisioning: NodeProvisioning

  override def toString: String = {
    "execution architecture" + "\t" + executionArchitecture + "\n" +
      "# nodes" + "\t" + "\t" + "\t" + numberOfNodes + "\n" +
      "nodes address" + "\t" + "\t" + nodesAddress + "\n"

  }
}

case class DefaultBootstrapConfiguration(
  executionArchitecture: ExecutionArchitecture = LocalExecutionArchitecture,
  numberOfNodes: Int = 1,
  nodesAddress: Vector[String] = Vector("localhost"),
  coordinatorAddress: String = "localhost",
  nodeProvisioning: NodeProvisioning = new EqualNodeProvisioning(Vector("localhost"), Runtime.getRuntime.availableProcessors)) extends BootstrapConfiguration

/**
 * Defines if the execution should take place locally or distributedly
 */
sealed trait ExecutionArchitecture extends Serializable

object LocalExecutionArchitecture extends ExecutionArchitecture {
  override def toString = "LocalArchitecture"
}
object DistributedExecutionArchitecture extends ExecutionArchitecture {
  override def toString = "DistributedArchitecture"
}