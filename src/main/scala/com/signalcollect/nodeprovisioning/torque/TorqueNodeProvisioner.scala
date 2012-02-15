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

import com.signalcollect.nodeprovisioning.NodeProvisioner
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

class TorqueNodeProvisioner(torqueHost: TorqueHost, system: ActorSystem) extends NodeProvisioner {
  def getNodes(numberOfNodes: Int): List[Node] = {
    
//    Not finished, not functional yet
    
//    val nodeProvisioner = system.actorOf(Props().withCreator(new NodeProvisionerActor()), name = "NodeProvisioner")
//    var jobs = List[TorqueJob]()
//    for (jobId <- 0 to numberOfNodes) {
//      val function () => Map[String, String] = 
//      jobs = new TorqueJob(jobId, nodeProvisioner.path) :: jobs
//    }
    List[Node]()
  }
}

class NodeProvisionerActor extends Actor {
  def receive = {
    case any => println("moo: " + any)
  }
}