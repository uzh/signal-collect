/*
 *  @author Daniel Strebel
 *  @author Philip Stutz
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

import scala.sys.process._
import ch.ethz.ssh2.Connection
import java.io.File
import ch.ethz.ssh2.StreamGobbler
import org.apache.commons.codec.binary.Base64
import scala.language.postfixOps

case class TorqueJobSubmitter(
    username: String,
    mailAddress: String = "",
    hostname: String,
    privateKeyFilePath: String = System.getProperty("user.home") + System.getProperty("file.separator") + ".ssh" + System.getProperty("file.separator") + "id_rsa",
    port: Int = 22) extends AbstractJobSubmitter(mailAddress) {

  override def copyFileToCluster(localPath: String, targetPath: String = "") {
    val commandCopy = "scp -v " + localPath + " " + username + "@" + hostname + ":" + targetPath
    println(commandCopy)
    println(commandCopy !!)
    Thread.sleep(1000) // wait a second to give NFS time to update and make the copied file visible
  }

  def executeCommandOnClusterManager(command: String): String = {
    val connection = connectToHost
    val session = connection.openSession
    session.execCommand(command)
    val result = IoUtil.streamToString(new StreamGobbler(session.getStdout)) + "\n" + IoUtil.streamToString(new StreamGobbler(session.getStderr))
    session.close
    connection.close
    result
  }

  protected def connectToHost: Connection = {
    val connection = new Connection(hostname, port)
    connection.connect
    connection.authenticateWithPublicKey(username, new File(privateKeyFilePath), null)
    connection
  }
}