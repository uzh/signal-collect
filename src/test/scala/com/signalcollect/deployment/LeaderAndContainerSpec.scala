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
  
  /**
   * Helper function for waiting on a condition for a given time
   */
  def waitOrTimeout(waitCondition: () => Boolean, untilIn10Ms: Int) {
    var cnt = 0
    while (waitCondition() && cnt < untilIn10Ms) {
      Thread.sleep(10)
      cnt += 1
    }
  }

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
      leader.isExecutionStarted === false
      leader.allNodeContainersRunning === false
      val address = s"akka.tcp://SignalCollect@$ip:2553/user/DefaultNodeActor$id"
      leaderActor ! address
      leader.waitForAllNodeContainers
      leader.allNodeContainersRunning === true

      val nodeActors = leader.getNodeActors
      nodeActors must not be empty
      nodeActors.head.path.toString === s"akka.tcp://SignalCollect@$ip:2553/user/DefaultNodeActor$id"
    }

    "filter address on DefaultNodeActor" in new LeaderScope {
      leader.clear
      val invalidAddress = "akka.tcp://SignalCollect@invalid"
      leaderActor ! invalidAddress
      leader.getNodeActors.isEmpty === true
    }

    "save shutdown address" in new LeaderScope {
      leader.clear
      val shutdownAddress = s"akka.tcp://SignalCollect@$ip:2553/user/shutdownactor$id"
      leaderActor ! shutdownAddress
      waitOrTimeout(() => leader.getShutdownActors.isEmpty, 500)
      leader.getShutdownActors.isEmpty === false
    }

    "clear ActorAddresses" in new LeaderScope {
      leader.clear
      leader.getNodeActors.isEmpty === true
      leader.getShutdownActors.isEmpty === true
    }

  }

  //integration of leader and container
  "Leader and ContainerNode" should {

    sequential //this is preventing the tests from being executed parallel
    "start execution when all nodes are registered" in new Execution {
      waitOrTimeout(() => !leader.isExecutionFinished, 500)
      leader.isExecutionFinished === true
      leader.isExecutionSuccessful === true
    }

    "get shutdownActors" in new LeaderContainerScope {
      waitOrTimeout(() => leader.getShutdownActors.isEmpty, 500)
      leader.getShutdownActors.size === 1
      leader.getShutdownActors.head.path.toString.contains("shutdown") === true
    }

    "shutdown after execution" in new Execution {
      waitOrTimeout(() => !container.shuttingdown, 500)
      container.shuttingdown === true
    }

    "get LeaderActor" in new LeaderContainerScope {
    	val leaderActor = container.getLeaderActor
    			leaderActor.path.toString === "akka://SignalCollect/user/leaderactor"
    }
  }

  "ContainerNode creation" should {
    sequential
    "be created" in new ContainerScope {
      container must not be None
    }
    "container node should start actor system" in new ContainerScope {
      ActorSystemRegistry.retrieve("SignalCollect").isDefined === true

    }

    "create shutdown actor" in new ContainerScope {
      val actor = container.getShutdownActor
      actor.toString.contains("SignalCollect/user/shutdownactor0") === true
    }

    "receive shutdown message" in new ContainerScope {
      container must not be None
      val actor = container.getShutdownActor
      actor ! "shutdown"
      waitOrTimeout(() => !container.shuttingdown, 500)
      container.shuttingdown === true
    }

    "wait for shutdown message" in new ContainerScope {
      container.shuttingdown === false
      container.isTerminated === false
      container.getShutdownActor ! "shutdown"
      waitOrTimeout(() => !container.isTerminated, 500)
      container.isTerminated === true
    }

    "get NodeActor" in new ContainerScope {
      container.getNodeActor must not be None
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
        system.awaitTermination(5.seconds)
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
  val config = DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf")
  val leader = LeaderCreator.getLeader(config).asInstanceOf[DefaultLeader[Int, Double]]
  val leaderActor: ActorRef = leader.leaderactor 

  abstract override def after {
    super.after
    leader.clear
  }
}

trait Execution extends LeaderContainerScope {
  var cnt = 0
  while (!leader.isExecutionFinished && cnt < 1000) {
    Thread.sleep(100)
    cnt += 1
  }
}

trait ContainerScope extends StopActorSystemAfter {
  val config = DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf")
  val leaderIp = InetAddress.getLocalHost().getHostAddress()
  val akkaConfig = AkkaConfigCreator.getConfig(2552, config)
  val container = new DefaultNodeContainer(id = 0,
    leaderIp = leaderIp,
    basePort = 2552,
    akkaConfig = akkaConfig,
    config)

}

trait LeaderContainerScope extends StopActorSystemAfter {
  val config = DeploymentConfigurationCreator.getDeploymentConfiguration("testdeployment.conf")
  val leaderIp = InetAddress.getLocalHost().getHostAddress()
  val akkaConfig = AkkaConfigCreator.getConfig(2552, config)
  val leader = new DefaultLeader(akkaConfig = akkaConfig, deploymentConfig = config)
  leader.start
  val container = new DefaultNodeContainer(id = 0,
    leaderIp = leaderIp,
    basePort = 2552,
    akkaConfig = akkaConfig,
    config)
  container.start

  abstract override def after {
    super.after
    leader.clear
  }

}

