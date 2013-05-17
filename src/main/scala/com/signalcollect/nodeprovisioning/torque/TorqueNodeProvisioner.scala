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

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.signalcollect.configuration.ActorSystemRegistry
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.nodeprovisioning.AkkaHelper
import com.signalcollect.nodeprovisioning.Node
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.typesafe.config.Config
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.Creator
import akka.pattern.ask
import akka.util.Timeout
import com.signalcollect.nodeprovisioning.DefaultNodeActor
import com.signalcollect.interfaces.NodeActor
import scala.reflect.ClassTag
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.nodeprovisioning.NodeActorCreator

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
case class NodeProvisionerCreator(numberOfNodes: Int) extends Creator[NodeProvisionerActor] {
  def create: NodeProvisionerActor = new NodeProvisionerActor(numberOfNodes)
}

class TorqueNodeProvisioner(
    torqueHost: TorqueHost,
    numberOfNodes: Int) extends NodeProvisioner {
  def getNodes(akkaConfig: Config): Array[ActorRef] = {
    val system: ActorSystem = ActorSystemRegistry.retrieve("SignalCollect").get
    val nodeProvisionerCreator = NodeProvisionerCreator(numberOfNodes)
    val nodeProvisioner = system.actorOf(Props[NodeProvisionerActor].withCreator(nodeProvisionerCreator.create), name = "NodeProvisioner")
    val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(nodeProvisioner, system)
    var jobs = List[Job]()
    implicit val timeout = new Timeout(Duration.create(1800, TimeUnit.SECONDS))
    for (jobId <- 0 until numberOfNodes) {
      val function: () => Unit = {
        () =>
          val system = ActorSystem("SignalCollect", akkaConfig)
          val nodeControllerCreator = NodeActorCreator(jobId, Some(nodeProvisionerAddress))
          val nodeController = system.actorOf(Props[DefaultNodeActor].withCreator(nodeControllerCreator.create), name = "DefaultNodeActor" + jobId.toString)
      }
      jobs = new Job(jobId = jobId, execute = function) :: jobs
    }
    torqueHost.executeJobs(jobs)
    val nodesFuture = nodeProvisioner ? "GetNodes"
    val result = Await.result(nodesFuture, timeout.duration)
    val nodes: Array[ActorRef] = result.asInstanceOf[List[ActorRef]].toArray
    nodes
  }
}
