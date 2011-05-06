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

package signalcollect.implementations.worker

import java.util.concurrent.ConcurrentHashMap
import signalcollect.interfaces._
import signalcollect.interfaces.Storage._
import signalcollect.interfaces.Queue._
import java.util.concurrent.TimeUnit
import java.util.concurrent.BlockingQueue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedHashSet
import util.collections.ConcurrentHashSet

class DirectDeliveryAsynchronousWorker(
  mb: MessageBus[Any, Any],
  messageInboxFactory: QueueFactory,
  storageFactory: StorageFactory
  ) extends AsynchronousWorker(mb, messageInboxFactory, storageFactory) {

  override def send(message: Any) = {
    message match {
      case s: Signal[_, _, _] => processSignal(s)
      case other => messageInbox.put(message)
    }
  }

  val pollingTimeoutNanoseconds: Long = 1000l * 1000l * 100l

  def poll {
    if (isIdle) {
      idlePoll
    } else {
      timeOutPoll
    }
  }

  def idlePoll {
    var messageReceived = false
    while (isIdle && !shutDown && (isConverged || isPaused)) {
      messageReceived = messageReceived || handleMessageIfAvailable(pollingTimeoutNanoseconds)
      if (messageReceived) {
    	  setIdle(false)
      }
      handlePauseAndContinue
    }
    setIdle(false)
  }

  def timeOutPoll {
    val waitStart = System.nanoTime
    val waitEnd = waitStart + idleTimeoutNanoseconds
    var messageReceived = false
    while (!shutDown && isConverged && !messageReceived && System.nanoTime < waitEnd) {
      messageReceived = messageReceived || handleMessageIfAvailable(pollingTimeoutNanoseconds)
    }
    setIdle(!messageReceived && (isConverged || isPaused))
  }

  protected override def deliverSignal(signal: Signal[_, _, _], vertex: Vertex[_, _]) {
    vertex.send(signal)
    vertexStore.toCollect+=vertex.id
  }

  override def handleIdling {
    handlePauseAndContinue
    if (isConverged || isPaused) {
      poll
    } else {
      processInbox
    }
  }

}