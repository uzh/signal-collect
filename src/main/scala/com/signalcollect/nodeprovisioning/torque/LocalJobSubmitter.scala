package com.signalcollect.nodeprovisioning.torque

import scala.sys.process._

class LocalJobSubmitter(mailAddress:String = "") extends AbstractJobSubmitter(mailAddress) {

  def executeCommandOnClusterManager(command: String): String = {
    command !!
  }
}