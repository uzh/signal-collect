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

package signalcollect.implementations.messaging

import signalcollect.interfaces._
import signalcollect._
import java.util.concurrent.TimeUnit
import java.util.concurrent.BlockingQueue

abstract class MessageRecipient[G](messageInboxFactory: () => BlockingQueue[G]) extends interfaces.MessageRecipient[G] {

  val messageInbox: BlockingQueue[G] = messageInboxFactory()

  override def send(message: G) = messageInbox.put(message)

  protected def processInbox = {
	  var message = messageInbox.poll(0, TimeUnit.NANOSECONDS)
	  while (message != null) {
	 	  process(message)
	 	  message = messageInbox.poll(0, TimeUnit.NANOSECONDS)
	  }
  }

  protected def handleMessage {
	val message = messageInbox.take
	process(message)
  }
  
  protected def handleMessageIfAvailable(waitTimeNanoseconds: Long = 0): Boolean = {
	val message = messageInbox.poll(waitTimeNanoseconds, TimeUnit.NANOSECONDS)
	if (message != null) {
		process(message)
		true
	}
	false
  } 
  
  protected def process(message: G)

}