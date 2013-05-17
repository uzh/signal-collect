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
import com.signalcollect.interfaces.NodeReady
import akka.actor.ActorLogging
import com.signalcollect.interfaces.ActorRestartLogging

class NodeProvisionerActor(numberOfNodes: Int)
  extends Actor
  with ActorLogging
  with ActorRestartLogging {

  var nodeListRequestor: Option[ActorRef] = None

  var nodeControllers = List[ActorRef]()

  def receive = {
    case "GetNodes" =>
      nodeListRequestor = Some(sender)
      sendNodesIfReady
    case NodeReady =>
      nodeControllers = sender :: nodeControllers
      sendNodesIfReady
  }

  def sendNodesIfReady {
    if (nodeControllers.size == numberOfNodes && nodeListRequestor.isDefined) {
      nodeListRequestor.get ! nodeControllers
      self ! PoisonPill
    }
  }
}