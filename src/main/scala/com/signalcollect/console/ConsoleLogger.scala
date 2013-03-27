/*
 *  @author Philip Stutz
 *  
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.console

import akka.actor.Actor
import akka.event.Logging.Debug
import akka.event.Logging.Error
import akka.event.Logging.Info
import akka.event.Logging.InitializeLogger
import akka.event.Logging.LoggerInitialized
import akka.event.Logging.Warning
import akka.event.Logging.LogEvent
import akka.event.Logging.LogLevel
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.Logger

case class Get(level: LogLevel, number: Int)

class ConsoleLogger extends Actor with Logger {
  var logMessages: List[LogEvent] = List()

  println(context.self)

  def getLogMessages(logLevel: LogLevel, numberOfMessages: Int): List[LogEvent] = {
    logMessages.filter(_.level == logLevel).take(numberOfMessages)
  }

  def receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case l @ Error(cause, logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Warning(logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Info(logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Debug(logSource, logClass, message) => logMessages = l :: logMessages
    case Request(command, reply) =>
      try {
        val result = command.asInstanceOf[Logger => Any](this)
        if (reply) {
          if (result != null) {
            sender ! result
          }
        }
      }
  }
}