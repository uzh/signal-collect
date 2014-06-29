package com.signalcollect.deployment

import com.signalcollect.configuration.ActorSystemRegistry

object LeaderApp extends App {
  val leader = LeaderCreator.getLeader(DeploymentConfigurationCreator.getDeploymentConfiguration)
  leader.start
}

object ContainerNodeApp extends App {
//  NodeKiller.killOtherMasterAndNodes
  println("start container")
  val id = args(0).toInt
  val ip = args(1)
  println("create Node")
  val container = ContainerNodeCreator.getContainer(id = id, leaderIp = ip)
  println("Node")
  container.start
  container.waitForTermination
  println("execution terminated")
  val system = ActorSystemRegistry.retrieve("SignalCollect")
  if (system.isDefined) {
    if (!system.get.isTerminated) {
      system.get.shutdown
    }
  }
}
