package com.signalcollect

import akka.event.Logging
import com.signalcollect.configuration.Akka
import com.typesafe.config.ConfigFactory

/**
 * Created by blingannagari on 29/09/15.
 */
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
      .withFallback(ConfigFactory.load().getConfig("signalcollect"))
      .withFallback(ConfigFactory.parseString(mappingsConfig))
      .withFallback(akkaConfig)
  }

  private[this] val akkaConfig = Akka.config(serializeMessages = Some(false),
    loggingLevel = Some(Logging.WarningLevel),
    kryoRegistrations = List.empty,
    kryoInitializer = Some("com.signalcollect.configuration.TestKryoInit"))
}
