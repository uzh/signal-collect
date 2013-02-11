/*
 *  @author Silvan Troxler
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

package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.interfaces.SignalMessage
import scala.collection.mutable.IndexedSeq
import scala.collection.mutable.Set
import scala.io.Source
import com.signalcollect.nodeprovisioning.torque.TorqueNodeProvisioner
import com.signalcollect.nodeprovisioning.torque.TorqueHost
import com.signalcollect.nodeprovisioning.torque.TorqueJobSubmitter
import com.signalcollect.nodeprovisioning.torque.TorquePriority



/**
 * SubType connection from one Type to another Type
 */
class SubType(t: Any) extends OnlySignalOnChangeEdge(t) {
  type Source = Type
  
  /**
   * Signaling the current state of a Type combined with its own ID
   */
  def signal = {
    source.state + (source.id.toString.toInt)
  }
}


/**
 * Type which is based on a DataFlowVertex
 */
class Type(vertexId: Any, initialState: Set[Int] = Set()) extends DataFlowVertex(vertexId, initialState) {
  type Signal = Set[Int]
  
  /**
   * Combination of the current state of a vertex and the collected signal
   */
  def collect(signal: Set[Int]): Set[Int] = {
    state ++ (signal)
  }

}

/**
 * Builds a tree consisting of several Types and SubType-connections, then executes the computation on that graph
 */
object TransitiveClosure extends App {
  
  // Data file not in the repository but can be obtained here: 
  // http://snap.stanford.edu/data/cit-HepPh.html
  val dataFile = "Cit-HepPh.txt"
  
  
  // cluster configuration
  val jobname  = "tc"
  val username = "stroxler" // System.getProperty("user.name")
  val numberOfNodes = 1
  val baseOptions = " -Xmx64000m" + " -Xms64000m" + " -Xmn8000m" + " -d64"
  val priority = TorquePriority.fast
  
  println("Connecting to Cluster...")
  val torqueJobSubmitter = new TorqueJobSubmitter(username, username + "@ifi.uzh.ch", "kraken.ifi.uzh.ch",
                                                  System.getProperty("user.home") + System.getProperty("file.separator")
                                                  + ".ssh" + System.getProperty("file.separator") + "id_rsa")
  
  // remove output file from last time and upload data file
  torqueJobSubmitter.executeCommandOnClusterManager("rm /home/user/" + username + "/out/" + jobname + ".out")
  torqueJobSubmitter.copyFileToCluster("Cit-HepPh.txt", "Cit-HepPh.txt")
  
  val torqueNodeProvisioner = new TorqueNodeProvisioner(
          													torqueHost = new TorqueHost(
      		  	  										  jobSubmitter = torqueJobSubmitter,
    			  	  									    localJarPath = "./target/signal-collect-2.1-SNAPSHOT.jar",
    			  	  									    priority = priority),
  			  	  									    numberOfNodes = numberOfNodes,
  			  	  									    jvmParameters = baseOptions) // + jvmParams)
  
  println("Building graph...")
  val graph = GraphBuilder
              .withConsole(true, 8088)
              .withNodeProvisioner(torqueNodeProvisioner)
              .build
  
  var i = 1
  for (line <- Source.fromFile(dataFile).getLines()) {
    if (!line.startsWith("#") && i<= 300000) { // limit number of edges
//    if (!line.startsWith("#")) {
      
      // split and trim values
      var citation = line.split("\\s+");
      citation(0) = citation(0).trim()
      citation(1) = citation(1).trim()
      
      // build graph
      graph.addVertex(new Type(citation(0)))
      graph.addVertex(new Type(citation(1)))
      graph.addEdge(citation(0), new SubType(citation(1)))
      
      i += 1
    }
  }

  println("Starting computation...")
  val stats = graph.execute
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
