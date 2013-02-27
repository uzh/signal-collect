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

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
case class NodeControllerCreator(jobId: Any, nodeProvisionerAddress: String) extends Creator[NodeControllerActor] {
  def create: NodeControllerActor = new NodeControllerActor(jobId, nodeProvisionerAddress)
}

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
case class NodeProvisionerCreator(numberOfNodes: Int) extends Creator[NodeProvisionerActor] {
  def create: NodeProvisionerActor = new NodeProvisionerActor(numberOfNodes)
}

class TorqueNodeProvisioner(torqueHost: TorqueHost, numberOfNodes: Int, jvmParameters: String) extends NodeProvisioner {
  def getNodes(akkaConfig: Config): List[Node] = {
    val system: ActorSystem = ActorSystemRegistry.retrieve("SignalCollect").get
    val nodeProvisionerCreator = NodeProvisionerCreator(numberOfNodes)
    val nodeProvisioner = system.actorOf(Props[NodeProvisionerActor].withCreator(nodeProvisionerCreator.create), name = "NodeProvisioner")
    val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(nodeProvisioner, system)
    var jobs = List[TorqueJob]()
    implicit val timeout = new Timeout(Duration.create(1800, TimeUnit.SECONDS))
    for (jobId <- 0 until numberOfNodes) {
      val function: () => Map[String, String] = {
        () =>
          val system = ActorSystem("SignalCollect", akkaConfig)
          val nodeControllerCreator = NodeControllerCreator(jobId, nodeProvisionerAddress)
          val nodeController = system.actorOf(Props[NodeControllerActor].withCreator(nodeControllerCreator.create), name = "NodeController" + jobId.toString)
          Map[String, String]()
      }
      jobs = new TorqueJob(jobId = jobId, execute = function, jvmParameters = jvmParameters) :: jobs
    }
    torqueHost.executeJobs(jobs)
    val nodesFuture = nodeProvisioner ? "GetNodes"
    val result = Await.result(nodesFuture, timeout.duration)
    val nodes: List[Node] = result.asInstanceOf[List[ActorRef]] map (AkkaProxy.newInstance[Node](_))
    nodes
  }
}