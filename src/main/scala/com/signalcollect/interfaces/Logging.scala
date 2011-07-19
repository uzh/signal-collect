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

import scala.annotation.elidable

trait Logging {
  
  protected def messageBus: MessageBus[_]
  
  lazy val from = this.toString
  
  @elidable(scala.annotation.elidable.ALL)
  def debug(msg: Any) = messageBus.sendToCoordinator(Debug(msg, from))

  @elidable(scala.annotation.elidable.CONFIG)
  def config(msg: Any) = messageBus.sendToCoordinator(Config(msg, from))

  @elidable(scala.annotation.elidable.INFO)
  def info(msg: Any) = messageBus.sendToCoordinator(Info(msg, from))

  @elidable(scala.annotation.elidable.WARNING)
  def warning(msg: Any) = messageBus.sendToCoordinator(Warning(msg, from))
  
  @elidable(scala.annotation.elidable.SEVERE)
  def severe(msg: Any) = messageBus.sendToCoordinator(Severe(msg, from))

}