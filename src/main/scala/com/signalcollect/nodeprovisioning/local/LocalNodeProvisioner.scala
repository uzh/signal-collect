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

package com.signalcollect.nodeprovisioning.local

import com.signalcollect.nodeprovisioning.Node
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.typesafe.config.Config
import akka.actor.ActorRef
import com.signalcollect.configuration.ActorSystemRegistry
import akka.actor.Props
import com.signalcollect.nodeprovisioning.DefaultNodeActor
import com.signalcollect.nodeprovisioning.NodeActorCreator
import com.signalcollect.interfaces.MessageBusFactory
import scala.reflect.ClassTag

class LocalNodeProvisioner()
    extends NodeProvisioner {
  def getNodes(akkaConfig: Config): Array[ActorRef] = {
    val system = ActorSystemRegistry.retrieve("SignalCollect").getOrElse(throw new Exception("No actor system with name \"SignalCollect\" found!"))
    if (system != null) {
      val nodeControllerCreator = NodeActorCreator(0, None)
      val nodeController = system.actorOf(Props[DefaultNodeActor].withCreator(nodeControllerCreator.create), name = "DefaultNodeActor")
      Array[ActorRef](nodeController)
    } else {
      Array[ActorRef]()
    }
  }
}