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

import com.signalcollect.nodeprovisioning.Node
import scala.util.Random
import java.io.File
import scala.sys.process._
import org.apache.commons.codec.binary.Base64
import com.signalcollect.serialization.DefaultSerializer
import java.io.FileOutputStream
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import ch.ethz.ssh2.StreamGobbler
import java.io.FileInputStream
import java.io.File
import ch.ethz.ssh2.Connection
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import java.util.concurrent.TimeUnit
import akka.dispatch.Future
import akka.dispatch.Await
import akka.actor.PoisonPill
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.signalcollect.configuration.AkkaConfig
import akka.serialization.SerializationExtension
import akka.serialization.JavaSerializer
import akka.actor.Address
import akka.actor.ExtendedActorSystem
import com.signalcollect.messaging.AkkaProxy
import akka.japi.Creator
import com.signalcollect.nodeprovisioning.AkkaHelper

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
  def getNodes: List[Node] = {
    val system: ActorSystem = ActorSystem("NodeProvisioner", AkkaConfig.get)
    val nodeProvisionerCreator = NodeProvisionerCreator(numberOfNodes)
    val nodeProvisioner = system.actorOf(Props().withCreator(nodeProvisionerCreator.create), name = "NodeProvisioner")
    val nodeProvisionerAddress = AkkaHelper.getRemoteAddress(nodeProvisioner, system)
    var jobs = List[TorqueJob]()
    implicit val timeout = new Timeout(1800 seconds)
    for (jobId <- 0 until numberOfNodes) {
      val function: () => Map[String, String] = {
        () =>
          val system = ActorSystem("SignalCollect", AkkaConfig.get)
          val nodeControllerCreator = NodeControllerCreator(jobId, nodeProvisionerAddress)
          val nodeController = system.actorOf(Props().withCreator(nodeControllerCreator.create), name = "NodeController" + jobId.toString)
          Map[String, String]()
      }
      jobs = new TorqueJob(jobId=jobId, execute=function, jvmParameters=jvmParameters) :: jobs
    }
    torqueHost.executeJobs(jobs)
    val nodesFuture = nodeProvisioner ? "GetNodes"
    val result = Await.result(nodesFuture, timeout.duration)
    val nodes: List[Node] = result.asInstanceOf[List[ActorRef]] map (AkkaProxy.newInstance[Node](_))
    nodes
  }
}