package com.signalcollect.nodeprovisioning.torque

import scala.sys.process._

class LocalJobSubmitter(mailAddress:String = "") extends AbstractJobSubmitter(mailAddress) {

  def executeCommandOnClusterManager(command: String): String = {
    println(command)
    command !!
  }
  
  def copyFileToCluster(localPath: String, targetPath: String = "") {
    val command = "cp " + localPath + " ~"
    println(command)
    command !!
  }
}