/*
 *  @author Philip Stutz
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

package com.signalcollect.nodeprovisioning.clustermeister

import collection.JavaConversions._
import akka.actor.ActorRef
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.nodeprovisioning.Node
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.signalcollect.configuration.AkkaConfig
import com.github.nethad.clustermeister.api.Clustermeister
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory
import com.signalcollect.nodeprovisioning.torque.NodeProvisionerCreator
import akka.actor.Props
import com.signalcollect.nodeprovisioning.AkkaHelper
import akka.pattern.ask
import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import com.signalcollect.nodeprovisioning.torque.NodeControllerCreator
import akka.dispatch.Await
import com.signalcollect.implementations.messaging.AkkaProxy
import java.util.concurrent.Callable
import com.typesafe.config.Config

class ClustermeisterNodeProvisioner extends NodeProvisioner {

  var cm: Option[Clustermeister] = None

  def getNodes: List[Node] = {
    try {
      cm = Some(ClustermeisterFactory.create)
      if (cm.isDefined) {
        val numberOfNodes = cm.get.getAllNodes.size
        val system: ActorSystem = ActorSystem("NodeProvisioner", AkkaConfig.get)
        val nodeProvisionerCreator = NodeProvisionerCreator(numberOfNodes)
        val nodeProvisioner = system.actorOf(Props().withCreator(nodeProvisionerCreator.create), name = "NodeProvisioner")
        val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(nodeProvisioner, system)
        implicit val timeout = new Timeout(1800 seconds)
        for (node <- cm.get.getAllNodes) {
          val actorNameFuture = node.execute(NodeControllerBootstrap(node.getID, nodeProvisionerAddress, AkkaConfig.get))
          println("Started node controller: " + actorNameFuture.get)
        }
        val nodesFuture = nodeProvisioner ? "GetNodes"
        val result = Await.result(nodesFuture, timeout.duration)
        val nodes: List[Node] = result.asInstanceOf[List[ActorRef]] map (AkkaProxy.newInstance[Node](_))
        nodes
      } else {
        throw new Exception("Clustermeister could not be initialized.")
      }
    } finally {
      if (cm.isDefined) {
        cm.get.shutdown
      }
    }
  }

}

case class NodeControllerBootstrap(nodeId: String, nodeProvisionerAddress: String, akkaConfig: Config) extends Callable[String] {
  def call: String = {
    val system = ActorSystem("SignalCollect", akkaConfig)
    val nodeControllerCreator = NodeControllerCreator(nodeId, nodeProvisionerAddress)
    val nodeController = system.actorOf(Props().withCreator(nodeControllerCreator.create), name = "NodeController" + nodeId)
    nodeController.toString + " is terminated: " + nodeController.isTerminated
  }
}