/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.implementations.logging
import com.signalcollect.interfaces._

object LoggingLevel {
  val Debug = 0
  val Config = 100
  val Info = 200
  val Warning = 300
  val Severe = 400
}

trait Logging {

  protected def messageBus: MessageBus[_]
  protected val loggingLevel: Int  // = LoggingLevel.Warning

  lazy val from = this.toString

  def debug(msg: Any) = {
    if (loggingLevel <= LoggingLevel.Debug) {
      messageBus.sendToCoordinator(Debug(msg, from))
    }
  }

  def config(msg: Any) = {
    if (loggingLevel <= LoggingLevel.Config) {
      messageBus.sendToCoordinator(Config(msg, from))
    }
  }

  def info(msg: Any) = {
    if (loggingLevel <= LoggingLevel.Info) {
      messageBus.sendToCoordinator(Info(msg, from))
    }
  }

  def warning(msg: Any) = {
    if (loggingLevel <= LoggingLevel.Warning) {
      messageBus.sendToCoordinator(Warning(msg, from))
    }
  }

  def severe(msg: Any) = {
    if (loggingLevel <= LoggingLevel.Severe) {
      messageBus.sendToCoordinator(Severe(msg, from))
    }
  }

}