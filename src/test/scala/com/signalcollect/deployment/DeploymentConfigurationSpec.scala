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

class DeploymentConfigurationSpec extends FlatSpec with Checkers {

  def createDeploymentConfiguration: DeploymentConfiguration = {
    val configAsString =
      """deployment {
	       memory-per-node = 512
	       jvm-arguments = "-XX:+AggressiveOpts"
	       number-of-nodes = 1
	       copy-files = ["some/file"]
	       algorithm = "com.signalcollect.deployment.PageRankExample"
	       algorithm-parameters {
		     "parameter-name" = "some-parameter"
	       }
	       cluster = "com.signalcollect.deployment.TestCluster"
           timeout = 400
           akka {
	         port: 2552
    		 kryo-initializer = "com.signalcollect.configuration.KryoInit"
    		 kryo-registrations = [
    		   "some.class.to.be.registered"
             ]
    	     serialize-messages = true
             loggers = [
    			"akka.event.Logging$DefaultLogger"
             ]
	       }
         }"""
    val config = ConfigFactory.parseString(configAsString)
    DeploymentConfigurationCreator.getDeploymentConfiguration(config)
  }

  "DeploymentConfiguration" should "contain algorithm" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.algorithm === "com.signalcollect.deployment.PageRankExample")
  }

  it should "contain algorithmParameters" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.algorithmParameters === Map[String, String]("parameter-name" -> "some-parameter"))
  }

  it should "contain memoryPerNode" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.memoryPerNode === 512)
  }

  it should "contain numberOfNodes" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.numberOfNodes === 1)
  }

  it should "contain copyFiles" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.copyFiles === List("some/file"))
  }

  it should "contain clusterType" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.cluster === "com.signalcollect.deployment.TestCluster")
  }

  it should "contain jvmArguments" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.jvmArguments === "-XX:+AggressiveOpts")
  }

  it should "contain timeout" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.timeout === 400)
  }
  
  it should "contain akka config" in {
    val deploymentConfig = createDeploymentConfiguration
    assert(deploymentConfig.akkaBasePort === 2552)
    assert(deploymentConfig.kryoInit === "com.signalcollect.configuration.KryoInit")
    assert(deploymentConfig.kryoRegistrations === List("some.class.to.be.registered"))
    assert(deploymentConfig.serializeMessages === true)
    assert(deploymentConfig.loggers === List("akka.event.Logging$DefaultLogger"))
  }
}