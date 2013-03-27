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

case class Get(level: LogLevel, number: Int)

class ConsoleLogger extends Actor {
  var logMessages: List[LogEvent] = List()

  println(context.self)
  
  def receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
    case l @ Error(cause, logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Warning(logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Info(logSource, logClass, message) => logMessages = l :: logMessages
    case l @ Debug(logSource, logClass, message) => logMessages = l :: logMessages
    case Get(logLevel, number) => sender ! logMessages.filter(_.level == logLevel).take(number)
  }
}