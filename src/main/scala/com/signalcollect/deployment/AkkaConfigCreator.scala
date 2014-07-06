/*
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

import com.signalcollect.configuration.AkkaConfig
import com.typesafe.config.Config

import akka.event.Logging
/**
 * Creates an akkaConfig out of the DeploymentConfiguration.
 * Port is passed seperate, because it is different on every machine
 */
object AkkaConfigCreator {
  /**
   * Creates an akkaConfig out of the DeploymentConfiguration.
   * Port is passed seperate, because it is different on every machine
   */
  def getConfig(port: Int, deploymentConfig: DeploymentConfiguration): Config = {
    val kryoRegistrations: List[String] = deploymentConfig.kryoRegistrations
    val kryoInit = deploymentConfig.kryoInit
    val serializeMessages = deploymentConfig.serializeMessages
    val loggers = deploymentConfig.loggers
    AkkaConfig.get(
      serializeMessages = serializeMessages,
      loggingLevel = Logging.DebugLevel, //Logging.DebugLevel,Logging.WarningLevel
      kryoRegistrations = kryoRegistrations,
      kryoInitializer = kryoInit,
      port = port,
      loggers = loggers)
  }
}