/*
 *  @author Philip Stutz
 *  @author Daniel Strebel
 *  @author Francisco de Freitas
 *  @author Lorenz Fischer
 *
 *  Copyright 2011 University of Zurich
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

import com.signalcollect.interfaces._
import com.signalcollect.serialization.DefaultSerializer
import java.io.FileInputStream
import java.io.File

object JobExecutor extends App {
  val job: Job = {
    if (args.size > 0) {
      try {
        val jobId = args(0).toInt
        val configFile = new File(jobId + ".config")
        val jobArray = new Array[Byte](configFile.length.toInt)
        val fileInputStream = new FileInputStream(configFile)
        fileInputStream.read(jobArray)
        DefaultSerializer.read[Job](jobArray)
      } catch {
        case e: Exception => throw new Exception("Could not load configuration: \n" + e.getMessage() + "\n" + e.getCause() + "\n" + e.getStackTrace)
      }
    } else {
      throw new Exception("No jobId specified.")
    }
  }
  job.execute()
}

