/*
 *  @author Tobias Bachmann
 *
 *  Copyright 2014 University of Zurich
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