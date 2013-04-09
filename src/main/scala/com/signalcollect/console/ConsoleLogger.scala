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
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import java.io.BufferedReader
import java.io.FileReader

case class Get(level: LogLevel, number: Int)

class ConsoleLogger extends Actor with Logger with ActorLogging {
//  println(context.self)

  def logFileName = "log_messages.txt"
  val logReader = new BufferedReader(new FileReader(logFileName))

  // number of lines to read per request
  val maxReadLines = 5000;
      
  def getLogMessages: List[String] = {
    readLog
  }
  
  def writeLog(message: String) {
    val fileWriter = new FileWriter(logFileName, true)
    try {
      fileWriter.write(message + "\n")
    }
    finally {
      fileWriter.close()
    }
  }
  
  def createJsonString(
      level: String,
      cause: Throwable,
      logSource: String,
      logClass: Class[_],
      message: Any): String = {
    val causeStr = {
      if (cause == null) ""
      else cause.getMessage()
    }
    val logClassStr = logClass.toString()
    val source = {
      if (logClassStr.startsWith("class akka.")) {
        "akka"
      }
      else if (logClassStr.startsWith("class com.signalcollect.")
          && !logClassStr.startsWith("class com.signalcollect.examples.")) {
        "sc"
      } else {
        "user"
      }
    }
    val date = (new SimpleDateFormat("YYYY-MM-dd HH:mm:ss:SSS")).format(Calendar.getInstance().getTime())
    val json = ("level" -> level) ~
               ("source" -> source) ~
               ("date" -> date) ~
               ("cause" -> causeStr) ~
               ("logSource" -> logSource) ~
               ("logClass" -> logClassStr) ~ 
               ("message" -> message.toString) 
    compact(render(json))
  }
  
  def resetLog {
    val fileWriter = new FileWriter(logFileName, false)
    try {
      fileWriter.write((new String()))
    }
    finally {
      fileWriter.close()
    }
  }
  
  def readLog: List[String] = {
    var logMessages: List[String] = List()
    if (!(new File(logFileName)).exists) {
      return logMessages
    }
    var readLines = 0
    while (logReader.ready() && readLines < maxReadLines) {
      logMessages = logMessages ::: List(logReader.readLine())
      readLines += 1
    }
    logMessages
  }

  def receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case l @ Error(cause, logSource, logClass, message) =>
      writeLog(createJsonString("Error", cause, logSource, logClass, message))
    case l @ Warning(logSource, logClass, message) =>
      writeLog(createJsonString("Warning", null, logSource, logClass, message))
    case l @ Info(logSource, logClass, message) =>
      writeLog(createJsonString("Info", null, logSource, logClass, message))
    case l @ Debug(logSource, logClass, message) => 
      writeLog(createJsonString("Debug", null, logSource, logClass, message))
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