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

class ClusterNodeProvisioner[Id, Signal](val numberOfNodes: Int) extends NodeProvisioner[Id, Signal] {

  def getNodes(system: ActorSystem, actorNamePrefix: String, akkaConfig: Config): Array[ActorRef] = {
    implicit val timeout = Timeout(300.seconds)
    //TODO: Is this coming from some config?
    val idleDetectionPropagationDelayInMilliseconds = 500
    val provisioner = system.actorOf(Props(classOf[ClusterNodeProvisionerActor],
      idleDetectionPropagationDelayInMilliseconds, actorNamePrefix, numberOfNodes),
      actorNamePrefix + "ClusterNodeProvisionerActor")
    val nodeActorsFuture = provisioner ? RetrieveNodeActors
    val nodeActors = Await.result(nodeActorsFuture, timeout.duration)
    nodeActors match {
      case n: Array[ActorRef] => n
      case _                  => throw new Exception(s"Expected an array of actor references, instead got $nodeActors.")
    }
  }

}
