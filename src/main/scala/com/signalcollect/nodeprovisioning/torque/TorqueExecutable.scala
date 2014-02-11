/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
 *  
 *  Copyright 2013 University of Zurich
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

import collection.JavaConversions._
import com.signalcollect.util.RandomString
import java.io.File
import java.net.InetAddress

object KrakenExecutable extends App {
  def assemblyPath = "./target/scala-2.10/signal-collect-2.1-SNAPSHOT.jar"
  val kraken = new TorqueHost(
    jobSubmitter = new TorqueJobSubmitter(username = System.getProperty("user.name"), hostname = "kraken.ifi.uzh.ch"),
    coresPerNode = 23,
    localJarPath = assemblyPath, priority = TorquePriority.fast)
  val executionHost = kraken
  val jobId = s"j-${RandomString.generate(6)}"
  executionHost.executeJobs(List(Job(2, TorqueExecutable.job, jobId)))
}

/**
 * On separate object to circumvent serialization issues.
 */
object TorqueExecutable {

  def job(): List[Map[String, String]] = {
    println(s"Job is being executed ...")
    val nodesFilePath = System.getenv("PBS_NODEFILE")
    println(s"Nodes file is: $nodesFilePath, opening ...")
    val nodes = io.Source.fromFile(nodesFilePath).getLines
    for (node <- nodes) {
      println(s"Node name: $node")
      val address = InetAddress.getByName(node)
      println(s"That node has IP: ${address.getHostAddress}")
      println(s"Local host name: ${InetAddress.getLocalHost.getHostName}")
    }
    println("Done.")
    List()
  }
}