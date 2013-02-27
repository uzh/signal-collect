package com.signalcollect.nodeprovisioning.torque

import org.apache.commons.codec.binary.Base64

abstract class AbstractJobSubmitter(mailAddress: String) extends Serializable {

  def runOnClusterNode(jobId: String, jarname: String, mainClass: String, priority: String = TorquePriority.superfast, jvmParameters: String, jdkBinPath: String = ""): String = {
    val script = getShellScript(jobId, jarname, mainClass, priority, jvmParameters, jdkBinPath, mailAddress)
    val scriptBase64 = Base64.encodeBase64String(script.getBytes).replace("\n", "").replace("\r", "")
    val qsubCommand = """echo """ + scriptBase64 + """ | base64 -d | qsub"""
    executeCommandOnClusterManager(qsubCommand)
  }

  def executeCommandOnClusterManager(command: String): String

  def copyFileToCluster(localPath: String, targetPath: String = "")

  def getShellScript(jobId: String, jarname: String, mainClass: String, priority: String, jvmParameters: String, jdkBinPath: String, mailAddress: String): String = {
    val script = """
#!/bin/bash
#PBS -N """ + jobId + """
#PBS -l nodes=1:ppn=23
""" + priority + """
#PBS -j oe
#PBS -m b
#PBS -m e
#PBS -m a
#PBS -V
#PBS -o out/""" + jobId + """.out
#PBS -e err/""" + jobId + """.err
""" + { if (mailAddress != null && mailAddress.length > 0) "#PBS -m a -M " + mailAddress else "" } + """

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