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

import com.signalcollect.nodeprovisioning.torque.TorqueJobSubmitter
import com.signalcollect.nodeprovisioning.torque.TorquePriority


/**
* Run the TransitiveClosure on a cluster
*/
object Runner extends App {
  
  val userName = "stroxler"
  val jobName  = "tc"
  
  // connect to the cluster
  val submitter = new TorqueJobSubmitter(userName,
      userName + "@ifi.uzh.ch",
      "kraken.ifi.uzh.ch",
      System.getProperty("user.home") + System.getProperty("file.separator")
        + ".ssh" + System.getProperty("file.separator") + "id_rsa")

  // remove output file from last time
  submitter.executeCommandOnClusterManager("rm /home/user/" + userName + "/out/" + jobName + ".out")

  // run the computation
  submitter.copyFileToCluster("target/signal-collect-2.1-SNAPSHOT.jar", jobName + ".jar")
  submitter.copyFileToCluster("Cit-HepPh.txt", "Cit-HepPh.txt")
  submitter.runOnClusterNode(jobName,
		  					 jobName + ".jar",
		  					 "com.signalcollect.examples.TransitiveClosure",
		  					 TorquePriority.superfast,
		  					 "-Xmx55G -Xms55G")
  
}