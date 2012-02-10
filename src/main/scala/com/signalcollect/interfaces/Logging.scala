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

import com.signalcollect.configuration.LoggingLevel

trait Logging {

  protected def messageBus: MessageBus
  protected val loggingLevel: Int // = LoggingLevel.Warning

  lazy val from = this.toString

  def debug(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Debug) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToLogger(Debug(msg :: msgs.toList, from))
      } else {
        messageBus.sendToLogger(Debug(msg, from))
      }
    }
  }

  def config(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Config) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToLogger(Config(msg :: msgs.toList, from))
      } else {
        messageBus.sendToLogger(Config(msg, from))
      }
    }
  }

  def info(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Info) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToLogger(Info(msg :: msgs.toList, from))
      } else {
        messageBus.sendToLogger(Info(msg, from))
      }
    }
  }

  def warning(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Warning) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToLogger(Warning(msg :: msgs.toList, from))
      } else {
        messageBus.sendToLogger(Warning(msg, from))
      }
    }
  }

  def severe(msg: Any, msgs: Any*) = {
    if (loggingLevel <= LoggingLevel.Severe) {
      if (msgs != null && !msgs.isEmpty) {
        messageBus.sendToLogger(Severe(msg :: msgs.toList, from))
      } else {
        messageBus.sendToLogger(Severe(msg, from))
      }
    }
  }

}