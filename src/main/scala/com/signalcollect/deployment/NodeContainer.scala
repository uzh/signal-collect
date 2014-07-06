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

import akka.actor.ActorSystem
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props
import scala.concurrent._
import com.signalcollect.node.DefaultNodeActor
import com.typesafe.config.Config
import scala.concurrent.duration._
import akka.actor.PoisonPill
import akka.actor.actorRef2Scala
import com.signalcollect.node.DefaultNodeActor
import com.signalcollect.util.AkkaRemoteAddress

trait NodeContainer {
  def start
  def shutdown
  def waitForTermination
  def isSuccessful: Boolean
}

class DefaultNodeContainer(id: Int,
  leaderIp: String,
  basePort: Int,
  akkaConfig: Config,
  deploymentConfig: DeploymentConfiguration) extends NodeContainer {

  val leaderAddress = s"akka.tcp://SignalCollect@$leaderIp:$basePort/user/leaderactor"
  val system = ActorSystemRegistry.retrieve("SignalCollect").getOrElse(startActorSystem)
  val shutdownActor = system.actorOf(Props(classOf[ShutdownActor], this), s"shutdownactor$id")
  val nodeActor = system.actorOf(Props(classOf[DefaultNodeActor],
    id.toString,
    id,
    deploymentConfig.numberOfNodes,
    None),
    name = id.toString + "DefaultNodeActor")

  private var terminated = false
  private var successful = false
  private var shutdownNow = false
  def isTerminated: Boolean = terminated
  def isSuccessful: Boolean = successful
  def shuttingdown: Boolean = shutdownNow

  def getShutdownActor(): ActorRef = {
    shutdownActor
  }

  def getNodeActor(): ActorRef = {
    nodeActor
  }

  def getLeaderActor(): ActorRef = {
    system.actorFor(leaderAddress)
  }

  def start {
    println("register at leader")
    getLeaderActor ! AkkaRemoteAddress.get(nodeActor, system)
    getLeaderActor ! AkkaRemoteAddress.get(shutdownActor, system)
  }

  def waitForTermination {
    val begin = System.currentTimeMillis()
    while (!shutdownNow && timeoutNotReached(begin) && terminated == false) {
      Thread.sleep(100)
    }
    terminated = true
  }

  def timeoutNotReached(begin: Long): Boolean = {
    val timeout = deploymentConfig.timeout
    (System.currentTimeMillis() - begin) / 1000 < timeout
  }

  def shutdown {
    terminated = true
    shutdownNow = true
    shutdownActor ! PoisonPill
    nodeActor ! PoisonPill
  }

  def startActorSystem: ActorSystem = {
    try {
      val system = ActorSystem("SignalCollect", akkaConfig)
      ActorSystemRegistry.register(system)
    } catch {
      case e: Throwable => {
        println("failed to start Actorsystem")
        throw e
      }
    }
    ActorSystemRegistry.retrieve("SignalCollect").get
  }
}

class ShutdownActor(container: DefaultNodeContainer) extends Actor {
  override def receive = {
    case "shutdown" => {
      container.shutdown
    }
    case whatever => println("received unexpected message")
  }
}