/*
 *  @author Philip Stutz
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

package com.signalcollect.factory.messagebus

import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.messaging.DefaultMessageBus
import com.signalcollect.messaging.BulkMessageBus
import scala.reflect.ClassTag

object AkkaMessageBusFactory extends MessageBusFactory {
  def createInstance[Id: ClassTag, Signal: ClassTag](numberOfWorkers: Int): MessageBus[Id, Signal] = new DefaultMessageBus[Id, Signal](numberOfWorkers)
  override def toString = "AkkaMessageBusFactory"
}

/**
 * Stores outgoing messages until 'flushThreshold' messages are queued for a worker.
 * Combines messages for the same vertex using 'combiner'.
 */
class BulkAkkaMessageBusFactory(flushThreshold: Int) extends MessageBusFactory {
  def createInstance[Id: ClassTag, Signal: ClassTag](numberOfWorkers: Int): MessageBus[Id, Signal] = new BulkMessageBus[Id, Signal](numberOfWorkers, flushThreshold)
  override def toString = "BulkAkkaMessageBusFactory"
}