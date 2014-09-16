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
import akka.event.Logging
import com.signalcollect.util.AkkaUtil

/**
 * Interface for the NodeContainer
 */
trait NodeContainer {
  def start
  def shutdown
  def waitForTermination
  def isSuccessful: Boolean
}

/**
 * Implementation of the NodeContainer
 */
class DefaultNodeContainer[Id, Signal](id: Int,
  leaderIp: String,
  basePort: Int,
  akkaConfig: Config,
  deploymentConfig: DeploymentConfiguration) extends NodeContainer {

  val leaderAddress = s"akka.tcp://SignalCollect@$leaderIp:$basePort/user/leaderactor"
  val system = ActorSystemRegistry.retrieve("SignalCollect").getOrElse(startActorSystem)
  val log = Logging.getLogger(system, this)
  val shutdownActor = system.actorOf(Props(classOf[ShutdownActor], this), s"shutdownactor$id")
  val nodeActor = system.actorOf(Props(classOf[DefaultNodeActor[Id, Signal]],
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
    AkkaUtil.getActorRefFromSelection(system.actorSelection(leaderAddress))
  }

  /**
   * Registers the nodeActor and the shutdownActor at the leader
   */
  def start {
    getLeaderActor ! AkkaRemoteAddress.get(nodeActor, system)
    getLeaderActor ! AkkaRemoteAddress.get(shutdownActor, system)
  }

  /**
   * Blocks until the leader sents the shutdown signal
   */
  def waitForTermination {
    val begin = System.currentTimeMillis()
    var cnt = 0
    while (!shutdownNow && timeoutNotReached(begin) && terminated == false) {
      val runtime = Runtime.getRuntime
      val memoryUsage: Double = 1.0 - (runtime.freeMemory().toDouble / runtime.maxMemory.toDouble)
      if (memoryUsage > 0.95 && cnt % 150 == 0) log.warning(s"memory used: $memoryUsage")
      cnt += 1
      Thread.sleep(500)
    }
    terminated = true
  }

  /**
   * check if timeout is not reached
   */
  def timeoutNotReached(begin: Long): Boolean = {
    val timeout = deploymentConfig.timeout
    (System.currentTimeMillis() - begin) / 1000 < timeout
  }

  /**
   * shutdown the actors
   */
  def shutdown {
    terminated = true
    shutdownNow = true
    shutdownActor ! PoisonPill
    nodeActor ! PoisonPill
  }

  /**
   * starts an ActorSystem
   */
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

/**
 * This Actor waits for the shutdown signal and then tells the NodeContainer to shutdown.
 */
class ShutdownActor(container: DefaultNodeContainer[_, _]) extends Actor {
  override def receive = {
    case "shutdown" => {
      container.shutdown
    }
    case whatever => println("received unexpected message")
  }
}
