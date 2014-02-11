/*
 *  @author Philip Stutz
 *  @author Thomas Keller
 *
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.nodeprovisioning.torque

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.actorRef2Scala
import akka.actor.ActorLogging
import com.signalcollect.interfaces.ActorRestartLogging
import akka.actor.ActorSystem
import com.signalcollect.configuration.ActorSystemRegistry
import com.signalcollect.nodeprovisioning.NodeActorCreator
import akka.actor.Props
import com.signalcollect.nodeprovisioning.DefaultNodeActor
import com.signalcollect.nodeprovisioning.AkkaHelper
import com.signalcollect.interfaces.GetNodes
import com.signalcollect.interfaces.NodeReady

class NodeProvisionerActor(
  numberOfNodes: Int,
  allocateWorkersOnCoordinatorNode: Boolean)
  extends Actor
  with ActorLogging
  with ActorRestartLogging {

  if (allocateWorkersOnCoordinatorNode) {
    val system = ActorSystemRegistry.retrieve("SignalCollect").
      getOrElse(throw new Exception("No actor system with name \"SignalCollect\" found!"))
    val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(self, system)
    val nodeControllerCreator = NodeActorCreator(0, Some(nodeProvisionerAddress))
    val nodeController: ActorRef = ??? 
//      system.actorOf(Props[DefaultNodeActor].withCreator(
//      nodeControllerCreator.create), name = "DefaultNodeActorOnCoordinatior")
  }

  var nodeArrayRequestor: Option[ActorRef] = None
  var nodesReady = 0
  var nodeControllers = new Array[ActorRef](numberOfNodes)

  def receive = {
    case GetNodes =>
      nodeArrayRequestor = Some(sender)
      sendNodesIfReady
    case NodeReady(nodeId) =>
      nodesReady += 1
      println(s"Received registration from $sender, $nodesReady/$numberOfNodes nodes ready.")
      nodeControllers(nodeId) = sender
      sendNodesIfReady
  }

  def sendNodesIfReady {
    if (nodesReady == numberOfNodes && nodeArrayRequestor.isDefined) {
      nodeArrayRequestor.get ! nodeControllers
      self ! PoisonPill
    }
  }
}