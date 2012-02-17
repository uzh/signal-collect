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
import com.signalcollect.implementations.serialization.DefaultSerializer
import java.io.FileOutputStream

case class TorqueHost(
  val torqueUsername: String = System.getProperty("user.name"),
  val torqueMailAddress: String = "",
  val torqueHostname: String = "kraken.ifi.uzh.ch",
  val jvmParameters: String = "",
  val recompileCore: Boolean = true,
  val jarDescription: String = Random.nextInt.abs.toString,
  val mainClass: String = "com.signalcollect.nodeprovisioning.torque.JobExecutor",
  val jarName: String = "signal-collect-core-2.0.0-SNAPSHOT",
  val priority: String = TorquePriority.superfast) extends TorqueJobSubmitter(torqueUsername, torqueMailAddress, torqueHostname) with ExecutionHost {

  lazy val jarSuffix = "-jar-with-dependencies.jar"
  lazy val fileSpearator = System.getProperty("file.separator")
  lazy val localhostJarname = jarName + jarSuffix
  lazy val krakenJarname = jarName + "-" + jarDescription + jarSuffix
  lazy val localJarpath = "." + fileSpearator + "target" + fileSpearator + localhostJarname

  def executeJobs(jobs: List[TorqueJob]) = {
    /** PACKAGE EVAL CODE AS JAR */
    val commandPackage = "mvn -Dmaven.test.skip=true clean package"
    println(commandPackage)
    println(commandPackage !!)

    /** COPY EVAL JAR TO TORQUE HOME DIRECTORY */
    copyFileToCluster(localJarpath, krakenJarname)

    /** SUBMIT AN EVALUATION JOB FOR EACH CONFIGURATION */
    for (job <- jobs) {
      val config = DefaultSerializer.write((job, resultHandlers))
      val out = new FileOutputStream(job.jobId + ".config")
      out.write(config)
      out.close
      copyFileToCluster(job.jobId + ".config")
      val deleteConfig = "rm " + job.jobId + ".config"
      deleteConfig !!
      
      runOnClusterNode(job.jobId.toString, krakenJarname, mainClass, priority, jvmParameters)
    }
  }
}