/*
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

package com.signalcollect.interfaces

import com.signalcollect._
import akka.actor.Actor
import com.signalcollect.coordinator.WorkerApi

/**
 * Required because a Java Dynamic Proxy can only work with interfaces
 */
trait Coordinator extends Actor with MessageRecipientRegistry with Logging {

  override def toString = this.getClass.getSimpleName

  def isIdle: Boolean

  def getWorkerApi: WorkerApi  //TODO remove dependency on class inside of an implementation package.
  
  def getGraphEditor: GraphEditor
  
  def globalInboxSize: Long
}

object Coordinator {
  // Returns the position of the number of messages sent to the coordinator in the message counter array.
  def getCoodinatorPosition(numberOfWorkers: Int): Int = numberOfWorkers
  // Returns the position of the number of messages sent to other actors in the message counter array.
  def getOthersPosition(numberOfWorkers: Int): Int = numberOfWorkers + 1
}