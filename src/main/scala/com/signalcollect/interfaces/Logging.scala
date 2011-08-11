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

package com.signalcollect.interfaces

object LoggingLevel {
  val Debug = 0
  val Config = 100
  val Info = 200
  val Warning = 300
  val Severe = 400
}

trait Logging {

  protected def messageBus: MessageBus[_]
  protected val loggingLevel: Int // = LoggingLevel.Warning

  lazy val from = this.toString

  def debug(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Debug) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToCoordinator(Debug(msg :: msgs.toList, from))
      } else {
        messageBus.sendToCoordinator(Debug(msg, from))
      }
    }
  }

  def config(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Config) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToCoordinator(Config(msg :: msgs.toList, from))
      } else {
        messageBus.sendToCoordinator(Config(msg, from))
      }
    }
  }

  def info(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Info) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToCoordinator(Info(msg :: msgs.toList, from))
      } else {
        messageBus.sendToCoordinator(Info(msg, from))
      }
    }
  }

  def warning(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Warning) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToCoordinator(Warning(msg :: msgs.toList, from))
      } else {
        messageBus.sendToCoordinator(Warning(msg, from))
      }
    }
  }

  def severe(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Severe) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToCoordinator(Severe(msg :: msgs.toList, from))
      } else {
        messageBus.sendToCoordinator(Severe(msg, from))
      }
    }
  }

}