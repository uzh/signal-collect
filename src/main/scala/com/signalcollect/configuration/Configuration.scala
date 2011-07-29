/*
 *  @author Francisco de Freitas
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

package com.signalcollect.configuration

import com.signalcollect.interfaces._

import java.util.HashMap

/**
 * Main configuration for Signal Collect. Used for constructing a [compute graph]
 * Use this for the Local Case
 */
trait Configuration {

  def numberOfWorkers: Int
  
  def maxInboxSize: Option[Long]

  def customLogger: Option[MessageRecipient[LogMessage]]

  def workerConfiguration: WorkerConfiguration

  def executionConfiguration: ExecutionConfiguration

}

case class DefaultLocalConfiguration(numberOfWorkers: Int = Runtime.getRuntime.availableProcessors,
									 maxInboxSize: Option[Long] = Some(Runtime.getRuntime.availableProcessors*5000),
                                     customLogger: Option[MessageRecipient[LogMessage]] = None,
                                     workerConfiguration: WorkerConfiguration = DefaultLocalWorkerConfiguration(),
                                     executionConfiguration: ExecutionConfiguration = DefaultExecutionConfiguration) extends Configuration
