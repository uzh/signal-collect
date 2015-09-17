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

  def printStats() = {
    /* Total amount of free memory available to the JVM */
    println("Free memory (MB): " + Runtime.getRuntime.freeMemory()/(1024*1024))

    val maxMemory = Runtime.getRuntime.maxMemory()
    /* Maximum amount of memory the JVM will attempt to use */
    println("Maximum memory (MB): " + maxMemory/(1024*1024))

    /* Total memory currently in use by the JVM */
    println("Total memory (MB): " + Runtime.getRuntime.totalMemory()/(1024*1024))
  }
}
