/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 Cotiviti
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

package com.signalcollect

import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.fixture.NoArg
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.signalcollect.configuration.Akka

object TestGraph {

  private[this] val uniquePrefixTracker = new AtomicInteger(0)

  private[this] val uniqueNameTracker = new AtomicInteger(0)

  def nextUniquePrefix = uniquePrefixTracker.incrementAndGet.toString

  def nextUniqueName = uniqueNameTracker.incrementAndGet.toString

//    private[this] def seed(actorSystemName: String, seedPort: Int, seedIp: String = "127.0.0.1") = ConfigFactory.parseString(
//    s"""akka.log-dead-letters-during-shutdown=off
//       |akka.clustering.name=$actorSystemName
//        |akka.clustering.seed-ip=$seedIp
//        |akka.clustering.seed-port=$seedPort
//        |akka.remote.netty.tcp.port=$seedPort
//        |akka.cluster.seed-nodes=["akka.tcp://"${actorSystemName}"@"${seedIp}":"${seedPort}]""".stripMargin)
//
//  def randomPort: Int = {
//    val socket = new ServerSocket(0)
//    val port = socket.getLocalPort
//    socket.close()
//    port
//  }
//
//  def actorSystem(name: String = UUID.randomUUID().toString.replace("-", ""), port: Int = randomPort) = {
//    ActorSystem(name, seed(name, port).withFallback(ConfigFactory.load().getConfig("signalcollect")))
//  }
  
  def instantiateUniqueActorSystem(): ActorSystem = {
    val defaultConfig = GraphBuilder.config
    val config = Akka.config(
      serializeMessages = None,
      loggingLevel = None,
      kryoRegistrations = defaultConfig.kryoRegistrations,
      kryoInitializer = None)
    ActorSystem(nextUniqueName, config)
  }

  def instantiateUniqueGraph(): Graph[Any, Any] = {
    val graphBuilder = GraphBuilder
      .withActorNamePrefix(nextUniquePrefix)
      .withActorSystem(instantiateUniqueActorSystem())
    graphBuilder.build
  }

}

class TestGraph(val g: Graph[Any, Any]) extends NoArg {

  def this() = this(TestGraph.instantiateUniqueGraph())

  def shutdown(): Unit = {
    g.shutdown
  }

  override def apply(): Unit = {
    try super.apply()
    finally shutdown()
  }

}
