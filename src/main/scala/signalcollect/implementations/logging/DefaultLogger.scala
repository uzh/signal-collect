/*
 *  @author Francisco de Freitas
 *  
 *  Copyright 2011 University of Zurich
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

package signalcollect.implementations.logging

import signalcollect.interfaces._
import signalcollect.interfaces.LogMessage
import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

class DefaultLogger extends Logger {
  
  def receive(loggingMessage: Any) {
    loggingMessage match {
      case Config(msg) => config(msg)
      case Info(msg) => info(msg)
      case Severe(msg) => severe(msg)
      case Debug(msg) => Debug(msg)
    }
  }
}

class ActorLogger extends Logger with Actor {
  
  def receive(message: Any) = sys.error("Receive should not be called.")
  
  def receive = {
    case Config(msg) => config(msg)
    case Info(msg) => info(msg)
    case Severe(msg) => severe(msg)
    case Debug(msg) => Debug(msg)
  }
}
