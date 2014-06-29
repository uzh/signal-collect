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

import java.net.InetAddress
import scala.async.Async.async
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import org.junit.runner.RunWith
import org.specs2.mutable.After
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LeaderAndContainerSpec extends SpecificationWithJUnit {

  sequential
  "Leader" should {
    sequential //this is preventing the tests from being executed parallel

    "be started" in new StopActorSystemAfter {
      val akkaPort = 2552
      val leader: Leader = LeaderCreator.getLeader(DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))
      ActorSystemRegistry.retrieve("SignalCollect").isDefined === true
    }

    "create LeaderActor" in new LeaderScope {
      leaderActor.path.toString.contains("leaderactor")
    }

    "detect if all nodes are ready " in new LeaderScope {
      leader.clear
      async { //is needed because waitForAllNodes is blocking
        leader.waitForAllNodes
      }
      leader.isExecutionStarted === false
      leader.allNodesRunning === false
      val address = s"akka.tcp://SignalCollect@$ip:2553/user/DefaultNodeActor$id"
      leaderActor ! address
      Thread.sleep(1000)
      leader.allNodesRunning === true
      leader.isExecutionStarted === true

      val nodeActors = leader.getNodeActorAddresses
      nodeActors must not be empty
      nodeActors.head === s"akka.tcp://SignalCollect@$ip:2553/user/DefaultNodeActor$id"
    }

    "filter address on DefaultNodeActor" in new LeaderScope {
      leader.clear
      async { //is needed because wait is blocking
        leader.waitForAllNodes
      }
      val invalidAddress = "akka.tcp://SignalCollect@invalid"
      leaderActor ! invalidAddress
      leader.getNodeActorAddresses.isEmpty === true
    }

    "save shutdown address" in new LeaderScope {
      leader.clear
      async { //is needed because wait is blocking
        leader.waitForAllNodes
      }
      val shutdownAddress = s"akka.tcp://SignalCollect@$ip:2553/user/shutdownactor$id"
      leaderActor ! shutdownAddress
      Thread.sleep(1000)
      leader.getShutdownAddresses.isEmpty === false
    }

    "clear ActorAddresses" in new LeaderScope {
      leader.clear
      leader.getNodeActorAddresses.isEmpty === true
      leader.getShutdownAddresses.isEmpty === true
    }

  }

  //integration of leader and container
  "Leader and ContainerNode" should {

    sequential //this is preventing the tests from being executed parallel
    "start execution when all nodes are registered" in new Execution {
      leader.isExecutionFinished === true
    }

    "get shutdownActors" in new LeaderContainerScope {
      Thread.sleep(1000)
      leader.getShutdownActors.size === 1
      leader.getShutdownActors.head.path.toString.contains("shutdown") === true
    }

    "shutdown after execution" in new Execution {
      Thread.sleep(1000)
      container.shuttingdown === true
    }

    "shutdown actorsystem after execution" in new Execution {
      Thread.sleep(10000)
      ActorSystemRegistry.retrieve("SignalCollect").isDefined === false
    }

  }
  "ContainerNode creation" should {
    sequential
    "be created" in new ContainerScope {
      container must not be None
    }
    "container node should start actor system" in new ContainerScope {
      container must not be None
      ActorSystemRegistry.retrieve("SignalCollect").isDefined === true

    }

    "create shutdown actor" in new ContainerScope {
      val actor = container.getShutdownActor
      actor must not be None
    }

    "receive shutdown message" in new ContainerScope {
      container.reset
      container must not be None
      val actor = container.getShutdownActor
      actor ! "shutdown"
      Thread.sleep(1000)
      container.shuttingdown === true
    }

    "wait for shutdown message" in new ContainerScope {
      container.reset
      container.shuttingdown === false
      async {
        container.waitForTermination
      }
      container.isTerminated === false
      container.getShutdownActor ! "shutdown"
      Thread.sleep(1000)
      container.isTerminated === true
    }

    "get NodeActor" in new ContainerScope {
      container.getNodeActor must not be None
    }

    "get LeaderActor" in new LeaderContainerScope {
      val leaderActor = container.getLeaderActor
      leaderActor.path.toString === "akka://SignalCollect/user/leaderactor"
    }

    "register with leader" in new LeaderContainerScope {
      container.register
      Thread.sleep(1000)
      leader.getNodeActorAddresses.exists(_.contains("DefaultNodeActor")) === true
      leader.getShutdownAddresses.exists(_.contains("shutdown")) === true
    }
  }

}

trait StopActorSystemAfter extends After {
  override def after = {
    ActorSystemRegistry.retrieve("SignalCollect") match {
      case Some(system) => clearSystem(system)
      case None =>
    }
  }

  def clearSystem(system: ActorSystem) {
    if (!system.isTerminated) {
      system.shutdown
      try {
        system.awaitTermination(20.seconds)
      } catch {
        case e: Exception => println("couldn't wait for shutdown of actorsystem")
      }
      ActorSystemRegistry.remove(system)
    }

  }
}

trait LeaderScope extends StopActorSystemAfter {
  val akkaPort = 2552
  val ip = InetAddress.getLocalHost.getHostAddress
  val id = 0
  val leader = LeaderCreator.getLeader(DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf")).asInstanceOf[DefaultLeader]
  val leaderActor: ActorRef = leader.getActorRef()

  abstract override def after {
    super.after
    leader.clear
  }
}

trait Execution extends LeaderContainerScope {
  Thread.sleep(1000)
  var cnt = 0
  while (!leader.isExecutionFinished && cnt < 1000) {
    Thread.sleep(100)
    cnt += 1
  }
  Thread.sleep(1000)
}

trait ContainerScope extends StopActorSystemAfter {
  val leaderIp = InetAddress.getLocalHost().getHostAddress()
  val akkaConfig = AkkaConfigCreator.getConfig(2552,DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))
  val container = new DefaultContainerNode(id = 0,
    leaderIp = leaderIp,
    basePort = 2552,
    akkaConfig = akkaConfig,
    DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))

}
trait LeaderContainerScope extends StopActorSystemAfter {
//  ActorAddresses.clear
//  ShutdownHelper.reset
  val leaderIp = InetAddress.getLocalHost().getHostAddress()
  val akkaConfig = AkkaConfigCreator.getConfig(2552,DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))
  val leader = new DefaultLeader(akkaConfig = akkaConfig, deploymentConfig = DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))
  leader.start
  val container = new DefaultContainerNode(id = 0,
    leaderIp = leaderIp,
    basePort = 2552,
    akkaConfig = akkaConfig,
    DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf"))
  container.start

  abstract override def after {
    super.after
    leader.clear
    container.reset
  }

}

