/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
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

package com.signalcollect.nodeprovisioning.torque

import org.apache.commons.codec.binary.Base64

abstract class AbstractJobSubmitter extends Serializable {

  def runOnClusterNodes(
    jobId: String,
    numberOfNodes: Int,
    coresPerNode: Int,
    jarname: String,
    mainClass: String,
    priority: String = TorquePriority.superfast,
    jvmParameters: String,
    jdkBinPath: String,
    mailAddress: Option[String] = None): String = {
    val script = getShellScript(jobId, numberOfNodes, coresPerNode, jarname, mainClass, priority, jvmParameters, jdkBinPath, mailAddress)
    val scriptBase64 = Base64.encodeBase64String(script.getBytes).replace("\n", "").replace("\r", "")
    val qsubCommand = """echo """ + scriptBase64 + """ | base64 -d | qsub"""
    executeCommandOnClusterManager(qsubCommand)
  }

  def executeCommandOnClusterManager(command: String): String

  def copyFileToCluster(localPath: String, targetPath: String = "")

  def getShellScript(
    jobId: String,
    numberOfNodes: Int,
    coresPerNode: Int,
    jarname: String,
    mainClass: String,
    priority: String,
    jvmParameters: String,
    jdkBinPath: String,
    mailAddress: Option[String]): String = {
    val script = """
#!/bin/bash
#PBS -N """ + jobId + """
#PBS -l nodes=""" + numberOfNodes + """:ppn=""" + coresPerNode + """
""" + priority + """
#PBS -j oe
#PBS -m b
#PBS -m e
#PBS -m a
#PBS -V
#PBS -o out/""" + jobId + """.out
#PBS -e err/""" + jobId + """.err
""" + { if (mailAddress.isDefined) "#PBS -m a -M " + mailAddress.get else "" } + """

jarname=""" + jarname + """
mainClass=""" + mainClass + """
workingDir=/home/torque/tmp/${USER}.${PBS_JOBID}
vm_args="""" + jvmParameters + """"

# copy jar
cp ~/$jarname $workingDir/

# run test
cmd="""" + jdkBinPath + """java $vm_args -cp $workingDir/$jarname $mainClass """ + jobId + """"
$cmd
"""
    script
  }
}
