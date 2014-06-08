/*
 *  @author Tobias Bachmann
 *
 *  Copyright 2013 University of Zurich
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
 */

package com.signalcollect.deployment

import org.scalatest.prop.Checkers
import org.scalatest.FlatSpec
import com.typesafe.config.ConfigFactory

class ClusterSpec extends FlatSpec with Checkers {

  def createDeploymentConfiguration(cluster: String): DeploymentConfiguration = {
    val configAsString =
      s"""deployment {
	       memory-per-node = 512
	       jvm-arguments = "-XX:+AggressiveOpts"
	       number-of-nodes = 1
	       copy-files = ["some/file"]
	       algorithm = "com.signalcollect.deployment.PageRankExample"
	       algorithm-parameters {
		     "parameter-name" = "some-parameter"
	       }
	       cluster = "${cluster}"
         }"""
    val config = ConfigFactory.parseString(configAsString)
    DeploymentConfigurationCreator.getDeploymentConfiguration(config)
  }
  
  def createCluster(config: DeploymentConfiguration): Cluster = {
    
    ClusterCreator.getCluster(config)
  }

  "Cluster" should "create and run" in {
    val deploymentConfiguration = createDeploymentConfiguration("com.signalcollect.deployment.TestCluster")
    val cluster = createCluster(deploymentConfiguration)
    assert(cluster.deploy(deploymentConfiguration) === true)
  }

}

class TestCluster extends Cluster {
  val successful = true
  override def deploy(deploymentConfiguration: DeploymentConfiguration): Boolean = {
	successful
  }
}