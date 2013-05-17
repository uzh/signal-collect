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

package com.signalcollect.interfaces

import akka.actor.{ Actor, ActorLogging }
import net.liftweb.json.JValue

trait Logger {
  def getLogMessages: List[JValue]
}

trait ActorRestartLogging {
  self: Actor with ActorLogging =>

  override def preRestart(t: Throwable, message: Option[Any]) {
    val msg = s"Unhandled error: $message"
    log.error(t, msg)
    println(msg)
    t.printStackTrace
  }
}