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

import com.signalcollect.configuration.ActorSystemRegistry
/**
 * This App starts a leader, which will wait then for NodeContainers to be registered and runs then a DeployableAlgorithm
 */
object LeaderApp extends App {
  val leader = LeaderCreator.getLeader(DeploymentConfigurationCreator.getDeploymentConfiguration)
  leader.start
}

/**
 * This App starts a NodeContainer, which will register itself at the leader
 * @param id each Container needs a unique id
 * @param ip is the IP address of the leader.
 */
object NodeContainerApp extends App {
  val id = args(0).toInt
  val ip = args(1)
  val container = NodeContainerCreator.getContainer(id = id, leaderIp = ip)
  container.start
  container.waitForTermination
  val system = ActorSystemRegistry.retrieve("SignalCollect")
  if (system.isDefined) {
    if (!system.get.isTerminated) {
      system.get.shutdown
    }
  }
}
