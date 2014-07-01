package com.signalcollect.deployment
/**
 * creates a new Leader according to the DeploymentConfiguration
 */
object LeaderCreator {
  def getLeader(deploymentConfig: DeploymentConfiguration = DeploymentConfigurationCreator.getDeploymentConfiguration): Leader = {
    val baseport = deploymentConfig.akkaBasePort
    val akkaConfig = AkkaConfigCreator.getConfig(baseport, deploymentConfig)
    new DefaultLeader(akkaConfig = akkaConfig,
        deploymentConfig = deploymentConfig)
  }
}