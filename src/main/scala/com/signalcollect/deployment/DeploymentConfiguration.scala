/*
 *  @author Tobias Bachmann
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

package com.signalcollect.deployment

import com.typesafe.config.ConfigFactory
import java.io.File
import scala.collection.JavaConversions._
import com.typesafe.config.Config

/**
 * All the deployment parameters
 */
case class DeploymentConfiguration(
  algorithm: String, //class name of a DeployableAlgorithm
  algorithmParameters: Map[String, String],
  memoryPerNode: Int = 512,
  numberOfNodes: Int = 1,
  copyFiles: List[String] = Nil, // list of paths to files
  clusterType: String = "yarn",
  jvmArguments: String = "")

/**
 * Creator of DeploymentConfiguration reads configuration from file 'deployment.conf'
 */
object DeploymentConfigurationCreator {
  val deployment = ConfigFactory.parseFile(new File("deployment.conf"))
  
  /**
   * creates DeploymentConfiguration out of 'deployment.conf'
   */
  def getDeploymentConfiguration: DeploymentConfiguration = getDeploymentConfiguration(deployment)
  
  /**
   * can be called with another Config, useful for testing or injecting another configuration than 'deployment.conf'
   */
  def getDeploymentConfiguration(config: Config): DeploymentConfiguration =
    new DeploymentConfiguration(
      algorithm = config.getString("deployment.algorithm"),
      algorithmParameters = getAlgorithmParameters(config),
      memoryPerNode = config.getInt("deployment.memory-per-node"),
      numberOfNodes = config.getInt("deployment.number-of-nodes"),
      copyFiles = config.getStringList("deployment.copy-files").toList, // list of paths to files
      clusterType = config.getString("deployment.type"),
      jvmArguments = config.getString("deployment.jvm-arguments"))

  private def getAlgorithmParameters(config: Config): Map[String, String] = {
    config.getConfig("deployment.algorithm-parameters").entrySet.map {
      entry => (entry.getKey, entry.getValue.unwrapped.toString)
    }.toMap
  }
}