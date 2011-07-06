/*
 *  @author Francisco de Freitas
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

package signalcollect.implementations.worker.akka

import signalcollect.api.Factory._
import signalcollect.interfaces._
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.actor.ReceiveTimeout

//class AkkaAsynchronousWorker(
//  workerId: Int,
//  mb: MessageBus[Any, Any],
//  mif: QueueFactory,
//  storageFactory: StorageFactory) extends AkkaWorker(workerId, mb, mif, storageFactory) {
//
//  //self.receiveTimeout = Some(100L)
//
//  var processedAll = true
//
//  def handlePauseAndContinue {
//    if (shouldStart) {
//      shouldStart = false
//      isPaused = false
//      messageBus.sendToCoordinator(StatusWorkerIsRunning(workerId))
//    }
//    if (shouldPause) {
//      shouldPause = false
//      isPaused = true
//      messageBus.sendToCoordinator(StatusWorkerIsPaused(workerId))
//    }
//  }
//
//  def receive = {
//
//    // FIXME still testing the correct effect of this
//    /*case ReceiveTimeout =>
//      // handleIdling
//      if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
//        if (mailboxIsEmpty)
//          setIdle(true)
//      }*/
//
//    case x =>
//      setIdle(false)
//      process(x)
//      handlePauseAndContinue
//
//      // handleIdling
//      if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
//        if (mailboxIsEmpty)
//          setIdle(true)
//      }
//
//      performComputation
//
//  }
//
//  def performComputation = {
//
//    // While the computation is in progress (work to do)
//    if (!isPaused) {
//
//      // alternately check the inbox and collect/signal
//      while (mailboxIsEmpty && !isConverged) {
//
//        if (processedAll) {
//          vertexStore.toSignal.foreach(vertex => signal(vertex))
//          processedAll = vertexStore.toCollect.foreachWithSnapshot(vertex => if (collect(vertex)) signal(vertex), () => { !mailboxIsEmpty })
//        } else
//          processedAll = vertexStore.toCollect.resumeProcessingSnapshot(vertex => if (collect(vertex)) signal(vertex), () => { !mailboxIsEmpty })
//
//      } // end while
//    } // !isPaused
//
//    if (processedAll && mailboxIsEmpty) setIdle(true)
//
//  }
//
//}