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
import akka.event.Logging
import akka.event.Logging.LogEvent
import akka.event.Logging.LogLevel
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.Logger
import java.io.FileWriter
import scala.io.Source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import akka.actor.ActorLogging

case class Get(level: LogLevel, number: Int)

class ConsoleLogger extends Actor with Logger with ActorLogging {
//  println(context.self)

  def getLogMessages(logLevel: LogLevel): List[String] = {
    readLog(logLevel)
  }
  
  def getLogFileName(logLevel: LogLevel): String = {
    logLevel match {
      case Logging.ErrorLevel   => "error_messages.txt"
      case Logging.WarningLevel => "warning_messages.txt"
      case Logging.InfoLevel    => "info_messages.txt"
      case Logging.DebugLevel   => "debug_messages.txt"
    }
  }
  
  def writeLog(logLevel: LogLevel, message: String) {
    val fileWriter = new FileWriter(getLogFileName(logLevel), true)
    try {
      val date = (new SimpleDateFormat("YYYY-MM-dd HH:mm:ss:SS")).format(Calendar.getInstance().getTime())
      fileWriter.write(date + " " + message + "\n")
    }
    finally {
      fileWriter.close()
    }
  }
  
  def resetLog(logLevel: LogLevel) {
    val fileWriter = new FileWriter(getLogFileName(logLevel), false)
    try {
      fileWriter.write((new String()))
    }
    finally {
      fileWriter.close()
    }
  }
  
  def readLog(logLevel: LogLevel): List[String] = {
    var logMessages: List[String] = List()
    val fileName = getLogFileName(logLevel)
    if (!(new File(fileName)).exists) {
      return logMessages
    }
    
    for(line <- Source.fromFile(fileName).getLines()) {
      if (line.length() > 0) {
        logMessages = logMessages ::: List(line)
      }
    }
    if (logMessages.size > 0) {
      resetLog(logLevel)
    }
    logMessages
  }

  def receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case l @ Error(cause, logSource, logClass, message) => writeLog(Logging.ErrorLevel, l.toString)
    case l @ Warning(logSource, logClass, message) => writeLog(Logging.WarningLevel, l.toString)
    case l @ Info(logSource, logClass, message) => writeLog(Logging.InfoLevel, l.toString)
    case l @ Debug(logSource, logClass, message) => writeLog(Logging.DebugLevel, l.toString)
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