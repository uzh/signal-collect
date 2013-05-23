/**
 *  @author Philip Stutz
 *  @author Silvan Troxler
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

/**
 * Handles the logging, it stores new messages and allows to retrieve them to send to the console.
 *
 * @constructor defines needed variables and values, clears the log file.
 */
class ConsoleLogger extends Actor with Logger with ActorLogging {

  /** Defines the name of the file in which log messages are stored. */
  def logFileName: String = "log_messages.txt"

  /** Stores the reader object used to read only new lines. */
  var logReader: BufferedReader = null

  /** Number of lines to read at most per request. */
  val maxReadLines = 1000;

  // reset log file
  resetLog

  /**
   * Writes the passed `message` to the log message file.
   *
   * @param message the log message to store
   */
  def writeLog(message: String) {
    val fileWriter = new FileWriter(logFileName, true)
    try {
      fileWriter.write(message + "\n")
    } finally {
      fileWriter.close()
    }
  }

  /**
   * Creates a JSON string based on the passed log message arguments.
   *
   * @param level the level of the log message
   * @param cause the cause of the log message
   * @param logSource the source of the log message
   * @param logClass the class of the log message
   * @param message the message of the log message
   * @return the string representing a JSON object.
   */
  def createJsonString(
    level: String,
    cause: String,
    logSource: String,
    logClass: Class[_],
    message: Any): String = {
    val logClassStr = logClass.toString()
    val source = {
      if (logClassStr.startsWith("class akka.")) {
        "akka"
      } else if (logClassStr.startsWith("class com.signalcollect.")
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
      ("cause" -> cause) ~
      ("logSource" -> logSource) ~
      ("logClass" -> logClassStr) ~
      ("message" -> message.toString)
    compact(render(json))
  }

  /**
   * Creates a JSON string based on the passed log message arguments.
   *
   * @param level the level of the log message
   * @param logSource the source of the log message
   * @param logClass the class of the log message
   * @param message the message of the log message
   * @return the string representing a JSON object.
   */
  def createJsonString(
    level: String,
    logSource: String,
    logClass: Class[_],
    message: Any): String = createJsonString(level, "", logSource, logClass, message)

  /** Clears the file in which log messages are stored. */
  def resetLog {
    if (logFileExists) {
      val fileWriter = new FileWriter(logFileName, false)
      try {
        fileWriter.write((new String()))
      } finally {
        fileWriter.close()
      }
    }
  }

  /**
   * Returns whether the log file exists.
   *
   * @return whether or not the log file exists.
   */
  def logFileExists: Boolean = {
    (new File(logFileName)).exists
  }

  /**
   * Returns new log messages added after the last request.
   *
   * @return a list of log messages.
   */
  def getLogMessages: List[JValue] = {
    var logMessages: List[JValue] = List()
    if (logFileExists) {
      if (logReader == null) {
        logReader = new BufferedReader(new FileReader(logFileName))
      }
      var readLines = 0
      while (logReader.ready() && readLines < maxReadLines) {
        logMessages = logMessages ::: List(parse(logReader.readLine()))
        readLines += 1
      }
    }
    logMessages
  }

  /**
   * Handles requests regarding logging.
   *
   * Forwards any log message type that should be stored, sends a list of log messages if these
   * are requested.
   */
  def receive: Receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case l @ Error(cause, logSource, logClass, message) =>
      writeLog(createJsonString("Error", cause.getMessage(), logSource, logClass, message))
    case l @ Warning(logSource, logClass, message) =>
      writeLog(createJsonString("Warning", logSource, logClass, message))
    case l @ Info(logSource, logClass, message) =>
      writeLog(createJsonString("Info", logSource, logClass, message))
    case l @ Debug(logSource, logClass, message) =>
      writeLog(createJsonString("Debug", logSource, logClass, message))
    case Request(command, reply, incrementor) =>
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
