/**
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

import scala.async.Async.async
import scala.concurrent.ExecutionContext.Implicits.global
import com.signalcollect.configuration.ActorSystemRegistry
import com.typesafe.config.Config
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import com.signalcollect.util.AkkaRemoteAddress

class DefaultLeader(
  akkaConfig: Config ,
  deploymentConfig: DeploymentConfiguration) extends Leader {
  val system = ActorSystemRegistry.retrieve("SignalCollect").getOrElse(startActorSystem)
  val leaderactor = system.actorOf(Props(classOf[LeaderActor], this), "leaderactor")
  val leaderAddress = AkkaRemoteAddress.get(leaderactor, system)
  private var executionStarted = false
  private var executionFinished = false

  def isExecutionStarted = executionStarted
  def isExecutionFinished = executionFinished

  def start {
    async {
      
      waitForAllNodes
      startExecution
      executionFinished = true
      shutdown
    }
  }

  def startExecution {
    val algorithm = deploymentConfig.algorithm
    val parameters = deploymentConfig.algorithmParameters
    val nodeActors = getNodeActors.toArray
    val algorithmObject = Class.forName(algorithm).newInstance.asInstanceOf[DeployableAlgorithm]
    println(s"start algorithm: $algorithm")
    algorithmObject.execute(parameters, Some(nodeActors), Some(system))
  }

  def shutdown {
    try {
      val shutdownActor = getShutdownActors.foreach(_ ! "shutdown")
      Thread.sleep(10000)
    } finally {
      if (!system.isTerminated) {
        system.shutdown
        system.awaitTermination
        ActorSystemRegistry.remove(system)
      }
    }
  }

  def getActorRef(): ActorRef = {
    leaderactor
  }

  def startActorSystem: ActorSystem = {
    try {
      val system = ActorSystem("SignalCollect", akkaConfig)
      ActorSystemRegistry.register(system)
      system
    } catch {
      case e: Exception => {
        throw e
      }
    }
  }

  def waitForAllNodes {
    while (!allNodesRunning) {
      Thread.sleep(100)
    }
    executionStarted = true
  }

  def allNodesRunning: Boolean = {
    
    getNumberOfNodes == deploymentConfig.numberOfNodes

  }

  def shutdownAllNodes {
    getShutdownActors.foreach(_ ! "shutdown")
  }

  def getNodeActors: List[ActorRef] = {
    val nodeActors = getNodeActorAddresses.map(nodeAddress => system.actorFor(nodeAddress))
    nodeActors
  }

  def getShutdownActors: List[ActorRef] = {
    val shutdownActors = getShutdownAddresses.map(address => system.actorFor(address))
    shutdownActors
  }
  
  private var nodeActorAddresses: List[String] = Nil
  private var shutdownAddresses: List[String] = Nil
  def getNodeActorAddresses: List[String] = {
    synchronized {
      nodeActorAddresses
    }
  }
  def getShutdownAddresses: List[String] = {
    synchronized {
      shutdownAddresses
    }
  }
  def addNodeActorAddress(address: String) {
    synchronized {
      (
        nodeActorAddresses = address :: nodeActorAddresses)
    }
  }
  def addShutdownAddress(address: String) {
    synchronized {
      (
        shutdownAddresses = address :: shutdownAddresses)
    }
  }
  def getNumberOfNodes: Int = {
    synchronized {
      nodeActorAddresses.size
    }
  }
  def clear = {
    synchronized {
      nodeActorAddresses = Nil
      shutdownAddresses = Nil
    }
  }

}

class LeaderActor(leader: DefaultLeader) extends Actor {
  override def receive = {
    case address: String => filterAddress(address)
    case _ => println("received unexpected message")
  }

  def filterAddress(address: String) {
    address match {
      case nodeactor if nodeactor.contains("NodeActor") => leader.addNodeActorAddress(address)
      case shutdown if shutdown.contains("shutdown") => leader.addShutdownAddress(address)
      case _ =>
    }
  }
}