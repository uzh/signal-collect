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
import com.signalcollect.implementations.serialization.DefaultSerializer
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

class TorqueNodeProvisioner(torqueHost: TorqueHost, numberOfNodes: Int) extends NodeProvisioner {
  def getNodes: List[Node] = {
    val system = ActorSystem("SignalCollect")
    val nodeProvisioner = system.actorOf(Props().withCreator(new NodeProvisionerActor(numberOfNodes)), name = "NodeProvisioner")
    var jobs = List[TorqueJob]()
    for (jobId <- 0 to numberOfNodes) {
      val function: () => Map[String, String] = {
        () =>
          val system = ActorSystem("SignalCollect", ConfigFactory.parseString(AkkaConfig.getConfig))
          val nodeController = system.actorOf(Props().withCreator(new NodeControllerActor(jobId, nodeProvisioner.path.toString)), name = "NodeController" + jobId.toString)
          Map[String, String]()
      }
      jobs = new TorqueJob(jobId, function) :: jobs
    }
    torqueHost.executeJobs(jobs)
    implicit val timeout = new Timeout(10 seconds)
    val nodesFuture = nodeProvisioner ? "GetNodes"
    val result = Await.result(nodesFuture, timeout.duration)
    result.asInstanceOf[List[Node]]
  }
}

class NodeProvisionerActor(numberOfNodes: Int) extends Actor {

  var nodeListRequestor: ActorRef = _

  var nodeControllers = List[ActorRef]()

  override def preStart() = {
    println("NodeProvisioner running")
  }

  def receive = {
    case "GetNodes" =>
      println("Received message `GetNodes` from " + sender)
      nodeListRequestor = sender
      sendNodesIfReady
    case "NodeReady" =>
      println("Received message `NodeReady` from " + sender)
      nodeControllers = sender :: nodeControllers
      sendNodesIfReady
  }

  def sendNodesIfReady {
    if (nodeControllers.size == numberOfNodes) {
      nodeListRequestor ! nodeControllers
      self ! PoisonPill
    }
  }
}

class NodeControllerActor(nodeId: Int, coordinatorAddress: String) extends Actor {

  var coordinator: ActorRef = _

  override def preStart() = {
    println("Job running: " + nodeId)
    coordinator = context.actorFor(coordinatorAddress)
    coordinator ! ("NodeReady", java.net.InetAddress.getLocalHost.getHostAddress)
  }

  def receive = {
    case any => println("moo: " + any)
  }
}