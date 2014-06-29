/**
 *  @author Tobias Bachmann
 *
 *  Copyright 2014 University of Zurich
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
package com.signalcollect.deployment

object ContainerNodeCreator {
  def getContainer(id: Int, leaderIp: String): ContainerNode = {
    val deploymentConfig = DeploymentConfigurationCreator.getDeploymentConfiguration
    val basePort = deploymentConfig.akkaBasePort
    val numberOfNodes = deploymentConfig.numberOfNodes
    val akkaConfig = AkkaConfigCreator.getConfig(basePort + id + 1, deploymentConfig)
    val container = new DefaultContainerNode(id = id,
      leaderIp = leaderIp,
      basePort = basePort,
      akkaConfig = akkaConfig,
      deploymentConfig = deploymentConfig)
    container
  }
}