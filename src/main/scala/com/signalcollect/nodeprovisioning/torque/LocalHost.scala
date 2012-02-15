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

class LocalHost extends ExecutionHost {
  def executeJobs(jobs: List[TorqueJob]) = {
    for (job <- jobs) {
      var statsMap = Map[String, String]()
      try {
        statsMap = job.execute()
        statsMap += (("evaluationDescription", job.jobDescription))
        statsMap += (("submittedByUser", job.submittedByUser))
        statsMap += (("jobId", job.jobId.toString))
        statsMap += (("executionHostname", java.net.InetAddress.getLocalHost.getHostName))
        statsMap += (("java.runtime.version", System.getProperties.get("java.runtime.version").toString))
        if (!resultHandlers.isEmpty) {
          for (resultHandler <- resultHandlers) {
            resultHandler.addEntry(statsMap)
          }
        } else {
          println(statsMap)
        }
        System.gc
      } catch {
        case e: Exception =>
          println(statsMap)
          println("resultHandlers" + resultHandlers)
          sys.error(e.getMessage + "\n" + e.getStackTraceString)
      }
    }
  }
}