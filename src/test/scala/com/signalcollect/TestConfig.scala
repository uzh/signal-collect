package com.signalcollect

import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object TestConfig {
  private[this] def seed(actorSystemName: String, seedPort: Int, seedIp: String = "127.0.0.1") = ConfigFactory.parseString(
    s"""akka.log-dead-letters-during-shutdown=off
       |akka.clustering.name=$actorSystemName
        |akka.clustering.seed-ip=$seedIp
        |akka.clustering.seed-port=$seedPort
        |akka.remote.netty.tcp.port=$seedPort
        |akka.cluster.seed-nodes=["akka.tcp://"${actorSystemName}"@"${seedIp}":"${seedPort}]""".stripMargin)

  def randomPort: Int = {
    // Private ports are those from 49152 through 65535
    val start = 49152
    val end = 65535
    val rnd = new scala.util.Random
    start + rnd.nextInt((end - start) + 1)
  }

  def actorSystem(name: String = UUID.randomUUID().toString.replace("-", ""), port: Int = randomPort) = {
    ActorSystem(name, seed(name, port).withFallback(ConfigFactory.load))
  }

  def prefix = UUID.randomUUID().toString.replace("-", "")
}
