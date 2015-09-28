/*
 *  @author Bharath Kumar
 *
 *  Copyright 2015 iHealth Technologies
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

package com.signalcollect

import akka.event.Logging
import com.signalcollect.configuration.Akka
import com.typesafe.config.ConfigFactory

object MultiJvmConfig {
  private[this] val seedIp = "127.0.0.1"
  val idleDetectionPropagationDelayInMilliseconds = 500

  def provisionerCommonConfig(seedPort: Int) = {
    ConfigFactory.parseString(
      s"""akka.remote.netty.tcp.port=$seedPort""".stripMargin)
  }

  def nodeCommonConfig(clusterName: String, seedPort: Int, mappingsConfig: String = "") = {
    ConfigFactory.parseString(
      s"""akka.testconductor.barrier-timeout=60s
          |akka.cluster.seed-nodes=["akka.tcp://"${clusterName}"@"${seedIp}":"${seedPort}]""".stripMargin)
      .withFallback(ConfigFactory.load())
      .withFallback(ConfigFactory.parseString(mappingsConfig))
      .withFallback(akkaConfig)
  }

  private[this] val akkaConfig = Akka.config(serializeMessages = Some(false),
    loggingLevel = Some(Logging.WarningLevel),
    kryoRegistrations = List.empty,
    kryoInitializer = Some("com.signalcollect.configuration.TestKryoInit"))
}
