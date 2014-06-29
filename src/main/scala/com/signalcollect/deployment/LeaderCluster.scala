/*
 *  @author Tobias Bachmann
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

package com.signalcollect.deployment

import java.net.InetAddress

class LeaderCluster extends Cluster {

  def deploy(deploymentConfiguration: DeploymentConfiguration): Boolean = {
    val leader = LeaderCreator.getLeader(deploymentConfiguration)
    leader.start
    val ip = InetAddress.getLocalHost().getHostAddress()
    val numberOfNodes = deploymentConfiguration.numberOfNodes 
    var id = 0
    for (id <- 0 until numberOfNodes) {
      println(s"create node $id")
      val container = ContainerNodeCreator.getContainer(id = id, leaderIp = ip)
      println(s"start node $id")
      container.start
    }
    while(!leader.isExecutionFinished){
      Thread.sleep(100)
    }
    true
  }
  
}



