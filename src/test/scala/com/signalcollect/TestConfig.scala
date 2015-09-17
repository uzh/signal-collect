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
