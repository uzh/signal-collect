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

package com.signalcollect.deployment

/**
 * Interface for Cluster where you can deploy an algorithm
 */
trait Cluster {
  /**
   * deploys the given configuration to the cluster and returns true when it was successful
   */
  def deploy(deploymentConfiguration: DeploymentConfiguration): Boolean
}
/**
 * creates a cluster from the class name given in DeploymentConfiguration.cluster
 */
object ClusterCreator {
  def getCluster(deploymentConfiguration: DeploymentConfiguration): Cluster = {
    val clusterClass = deploymentConfiguration.cluster
    try {
      Class.forName(clusterClass).newInstance.asInstanceOf[Cluster]
    } catch {
      case e: ClassNotFoundException => if (clusterClass == "com.signalcollect.deployment.yarn.YarnCluster")
        throw new ClassNotFoundException("Class for YarnCluster could not be found. Make sure you are using signal-collect-yarn project as dependency.")
      else
        throw new ClassNotFoundException("Class for Cluster could not be found. Make sure class specified in deployment.conf exists.")
    }
  }
}

