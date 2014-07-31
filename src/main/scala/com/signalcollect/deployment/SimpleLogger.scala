package com.signalcollect.deployment

import akka.actor.ActorSystem
import akka.event.Logging

trait SimpleLogger {
  def error(message: Any)
  def warn(message: Any)
  def info(message: Any)
  def debug(message: Any)
}

/**
 * a very simple implementation of a Logger
 */
object SimpleConsoleLogger extends SimpleLogger {
  def error(message: Any) {
    println(message)
  }
  def warn(message: Any) {
    println(message)
  }
  def info(message: Any) {
    println(message)
  }
  def debug(message: Any) {
    println(message)
  }
}

/**
 * Wraps a newly created AkkaLogger with the name passed in
 */
class SimpleAkkaLogger(system: ActorSystem, name: String) extends SimpleLogger {
  val log = Logging.getLogger(system, name)
  def error(message: Any) {
    log.error(message.toString)
  }
  def warn(message: Any) {
    log.warning(message.toString)
  }
  def info(message: Any) {
    log.info(message.toString)
  }
  def debug(message: Any) {
    log.debug(message.toString)
  }
}