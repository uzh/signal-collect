/*
 *  @author Philip Stutz
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
 *
 */

package com.signalcollect.nodeprovisioning.cluster

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.typesafe.config.{ Config, ConfigFactory }

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout

class ClusterNodeProvisioner[Id, Signal](
    numberOfNodes: Int,
    config: Config = ConfigFactory.load(),
    actorNamePrefix: String = "") extends NodeProvisioner[Id, Signal] {

  val system = ActorSystem("Signal/Collect", config)

  def getNodes(localSystem: ActorSystem, actorNamePrefix: String, akkaConfig: Config): Array[ActorRef] = {
    implicit val timeout = Timeout(300.seconds)
    val master = system.actorOf(Props(classOf[ClusterNodeProvisionerActor], actorNamePrefix + "ClusterMasterBootstrap", numberOfNodes),
      actorNamePrefix + "ClusterMasterBootstrap")
    val nodeActorsFuture = master ? RetrieveNodeActors
    val nodeActors = Await.result(nodeActorsFuture, timeout.duration)
    nodeActors match {
      case n: Array[ActorRef] => n
      case _                  => throw new Exception(s"Expected an array of actor references, instead got $nodeActors.")
    }
  }

}
