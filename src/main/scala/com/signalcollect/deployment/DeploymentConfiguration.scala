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

trait DeploymentConfiguration {
  def algorithm: String
  def algorithmParameters: Map[String, String]
  def memoryPerNode: Int
  def numberOfNodes: Int
  def copyFiles: List[String]
  def cluster: String
  def jvmArguments: String
  def timeout: Int
  def akkaBasePort: Int
}

/**
 * All the basic deployment parameters
 */
case class BasicDeploymentConfiguration(
  override val algorithm: String, //class name of a DeployableAlgorithm
  override val algorithmParameters: Map[String, String],
  override val memoryPerNode: Int = 512,
  override val numberOfNodes: Int = 1,
  override val copyFiles: List[String] = Nil, // list of paths to files
  override val cluster: String = "com.signalcollect.deployment.LeaderCluster",
  override val jvmArguments: String = "",
  override val timeout: Int = 400,
  override val akkaBasePort: Int = 2552) extends DeploymentConfiguration

/**
 * Creator of DeploymentConfiguration reads configuration from file 'deployment.conf'
 */
object DeploymentConfigurationCreator {
  val testdeployment = ConfigFactory.parseFile(new File("testdeployment.conf"))
  val deployment = ConfigFactory.parseFile(new File("deployment.conf")).withFallback(testdeployment)

  /**
   * creates DeploymentConfiguration out of 'deployment.conf'
   */
  def getDeploymentConfiguration: DeploymentConfiguration = getDeploymentConfiguration(deployment)

  /**
   * can be called with another Config, useful for testing or injecting another configuration than 'deployment.conf'
   */
  def getDeploymentConfiguration(config: Config): DeploymentConfiguration =
    new BasicDeploymentConfiguration(
      algorithm = config.getString("deployment.algorithm"),
      algorithmParameters = getAlgorithmParameters(config),
      memoryPerNode = config.getInt("deployment.memory-per-node"),
      numberOfNodes = config.getInt("deployment.number-of-nodes"),
      copyFiles = config.getStringList("deployment.copy-files").toList, // list of paths to files
      cluster = config.getString("deployment.cluster"),
      jvmArguments = config.getString("deployment.jvm-arguments"),
      timeout = config.getInt("deployment.timeout"),
      akkaBasePort = config.getInt("deployment.akka.port"))

  /**
   * useful for testing or injecting another configuration than 'deployment.conf'
   */
  def getDeploymentConfiguration(configPath: String): DeploymentConfiguration = {
    val config = ConfigFactory.parseFile(new File(configPath))
    getDeploymentConfiguration(config)
  }

  /**
   * extracts the algorithmParameters from the typesafe config
   */
  private def getAlgorithmParameters(config: Config): Map[String, String] = {
    config.getConfig("deployment.algorithm-parameters").entrySet.map {
      entry => (entry.getKey, entry.getValue.unwrapped.toString)
    }.toMap
  }
}