/*
 *  @author Tobias Bachmann
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
 * This is a simple implementation of a Cluster. Useful for testing an algorithm. 
 * It ignores most of the parameters in the DeploymentConfiguration.
 * It only takes the algorithm and the algorithmParameters for the execution 
 */
class LocalCluster[Id, Signal] extends Cluster {

  override def deploy(deploymentConfiguration: DeploymentConfiguration): Boolean = {
   val algorithm = deploymentConfiguration.algorithm
    val parameters = deploymentConfiguration.algorithmParameters
    val clazz = Class.forName(algorithm)
    val algorithmObject = clazz.getField("MODULE$").get(classOf[Algorithm[Id, Signal]]).asInstanceOf[Algorithm[Id, Signal]]
    println(s"start algorithm: $algorithm")
    algorithmObject.lifecycle(parameters, None, None)
    true
  }
}



