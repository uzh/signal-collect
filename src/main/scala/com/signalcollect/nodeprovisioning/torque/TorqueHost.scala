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

import scala.util.Random
import java.io.File
import scala.sys.process._
import org.apache.commons.codec.binary.Base64
import com.signalcollect.serialization.DefaultSerializer
import java.io.FileOutputStream
import java.net.URI

case class TorqueHost(
  torqueHostname: String,
  localJarPath: String,
  torqueUsername: String = System.getProperty("user.name"),
  torqueMailAddress: String = "",
  jarDescription: String = (Random.nextInt.abs % 1000).toString,
  mainClass: String = "com.signalcollect.nodeprovisioning.torque.JobExecutor",
  priority: String = TorquePriority.superfast,
  privateKeyFilePath: String = System.getProperty("user.home") + System.getProperty("file.separator") + ".ssh" + System.getProperty("file.separator") + "id_rsa") extends ExecutionHost {

  val fileSeparator = System.getProperty("file.separator")
  val jobSubmitter = new TorqueJobSubmitter(torqueUsername, torqueMailAddress, torqueHostname, privateKeyFilePath)
  val jarName = localJarPath.substring(localJarPath.lastIndexOf(fileSeparator) + 1, localJarPath.size)

  def executeJobs(jobs: List[TorqueJob]) = executeJobs(jobs, true)

  def executeJobs(jobs: List[TorqueJob], copyExecutable: Boolean = true) = {
    /** COPY EVAL JAR TO TORQUE HOME DIRECTORY */
    if (copyExecutable) {
      jobSubmitter.copyFileToCluster(localJarPath)
    }

    /** SUBMIT AN EVALUATION JOB FOR EACH CONFIGURATION */
    for (job <- jobs) {
      val config = DefaultSerializer.write((job, resultHandlers))
      val folder = new File("." + fileSeparator + "config-tmp")
      if(!folder.exists) {
        folder.mkdir
      }
      val configPath = "." + fileSeparator + "config-tmp" + fileSeparator + job.jobId + ".config"
      val out = new FileOutputStream(configPath)
      out.write(config)
      out.close
      jobSubmitter.copyFileToCluster(configPath)
      val deleteConfig = "rm " + configPath
      deleteConfig !!

      jobSubmitter.runOnClusterNode(job.jobId.toString, jarName, mainClass, priority, job.jvmParameters, job.jdkBinPath)
    }
  }
}