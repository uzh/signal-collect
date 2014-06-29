package com.signalcollect.deployment

object LeaderCreator {
  def getLeader(deploymentConfig: DeploymentConfiguration = DeploymentConfigurationCreator.getDeploymentConfiguration): Leader = {
    val baseport = deploymentConfig.akkaBasePort
    val akkaConfig = AkkaConfigCreator.getConfig(baseport, deploymentConfig)
    new DefaultLeader(akkaConfig = akkaConfig,
        deploymentConfig = deploymentConfig)
  }
}