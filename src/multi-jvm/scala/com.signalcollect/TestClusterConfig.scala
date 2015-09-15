package com.signalcollect

import com.typesafe.config.ConfigFactory

object TestClusterConfig {
  private[this] val seedIp = "127.0.0.1"
  private[this] val seedPort = 2556
  val idleDetectionPropagationDelayInMilliseconds = 500

  def provisionerCommonConfig = {
    ConfigFactory.parseString(
      s"""akka.remote.netty.tcp.port=$seedPort""".stripMargin)
  }

  def nodeCommonConfig(clusterName: String) = {
    ConfigFactory.parseString(
      s"""akka.cluster.seed-nodes=["akka.tcp://"${clusterName}"@"${seedIp}":"${seedPort}]""".stripMargin)
      .withFallback(ConfigFactory.load())
  }
}
