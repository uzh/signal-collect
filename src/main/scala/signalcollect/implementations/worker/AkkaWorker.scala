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

import akka.actor.Actor
import akka.dispatch._
import Actor._
import akka.actor.ReceiveTimeout
import signalcollect.implementations._
import signalcollect.interfaces._
import signalcollect.configuration._
import signalcollect.implementations.coordinator.WorkerApi

class AkkaWorker(workerId: Int,
  config: Configuration,
  coordinator: WorkerApi,
  mapper: VertexToWorkerMapper)
  extends LocalWorker(workerId, config, coordinator, mapper)
  with Actor {
  
  override def initialize {    
    self.start
  }

  self.receiveTimeout = Some(5L)
  
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  override def shutdown = {
    self.stop()
  }

  var processedAll = true

  def receive = {

    case ReceiveTimeout =>
      // idle handling
      if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
        if (mailboxIsEmpty)
          setIdle(true)
      }

    case x =>
      setIdle(false)
      process(x)
      handlePauseAndContinue

      // idle handling
      /*if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
        if (mailboxIsEmpty)
          setIdle(true)
      }*/

      performComputation

  }

  def performComputation = {

    // While the computation is in progress (work to do)
    if (!isPaused) {

      // alternately check the inbox and collect/signal
      while (mailboxIsEmpty && !isConverged) {

        //if (processedAll) {
          vertexStore.toSignal.foreach(vertex => signal(vertex))
          processedAll = vertexStore.toCollect.foreachWithSnapshot(vertex => if (collect(vertex)) signal(vertex), () => { false })
        //} else
        //  processedAll = vertexStore.toCollect.foreachWithSnapshot(vertex => if (collect(vertex)) signal(vertex), () => { !mailboxIsEmpty })

      } // end while
    } // !isPaused

    //if (processedAll && mailboxIsEmpty) setIdle(true)

  }

  /**
   * Checks if the Actor mailbox is empty
   */
  def mailboxIsEmpty: Boolean = if (self == null) true else self.dispatcher.mailboxIsEmpty(self)

  override def receive(message: Any) = sys.error("Receive should not be called from Akka Workers. This receive is not the same one from Akka.")

  /* protected def process(message: Any) {
    counters.messagesReceived += 1
    message match {
      case s: Signal[_, _, _] => processSignal(s)
      case WorkerRequest(command) => command(this)
      case other => log("Could not handle message " + message, "DEBUG")
    }
  }*/

}