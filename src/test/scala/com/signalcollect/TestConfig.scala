package com.signalcollect

import java.net.ServerSocket
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
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }

  def actorSystem(name: String = UUID.randomUUID().toString.replace("-", ""), port: Int = randomPort) = {
    ActorSystem(name, seed(name, port).withFallback(ConfigFactory.load))
  }

  def prefix = UUID.randomUUID().toString.replace("-", "")

  def graphProvider(systemName: String = UUID.randomUUID().toString.replace("-", "")) = GraphBuilder
    .withActorSystem(actorSystem(systemName))
    .withActorNamePrefix(prefix)
}
