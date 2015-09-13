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

  def actorSystem(name: String = UUID.randomUUID().toString, port: Int) = {
    ActorSystem(name, seed(name, port).withFallback(ConfigFactory.load))
  }
}
