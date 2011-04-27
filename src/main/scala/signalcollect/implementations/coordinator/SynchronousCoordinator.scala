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

package signalcollect.implementations.coordinator

import signalcollect.interfaces.Queue._
import signalcollect.interfaces.Worker._
import signalcollect.interfaces.MessageBus._
import signalcollect.interfaces.ComputationStatistics
import signalcollect.implementations.logging.SeparateThreadLogger
import signalcollect.interfaces._
import java.util.concurrent.BlockingQueue

class SynchronousCoordinator(
  numberOfWorkers: Int,
  workerFactory: WorkerFactory,
  messageInboxFactory: QueueFactory,
  messageBusFactory: MessageBusFactory,
  logger: Option[MessageRecipient[Any]] = None
  ) extends AbstractCoordinator(numberOfWorkers, workerFactory, messageInboxFactory, messageBusFactory, logger) {

  var steps = 0
  
  var stepsLimit = Int.MaxValue
  
  def performComputation: collection.mutable.Map[String, Any] = {
    do {
      executeComputationStep
    } while ((steps < stepsLimit) && collectStep().get > 0l)
    log("Steps: " + steps)
    val statsMap = collection.mutable.LinkedHashMap[String, Any]()
    messageBus.sendToWorkers(CommandSendComputationProgressStats)
    handleMessage
    messageBus.sendToWorkers(CommandSendComputationProgressStats)
    handleMessage
    computationProgressStatisticsSecondPass = computationProgressStatistics
    statsMap.put("signalCollectSteps", steps)
    statsMap
  }
  
  def executeComputationStep {
    steps += 1
    signalStep.reset
    collectStep.reset
    messageBus.sendToWorkers(CommandSignalStep)
    while (!signalStep.isDone) {
      handleMessage
    }
    messageBus.sendToWorkers(CommandCollectStep)
    while (!collectStep.isDone) {
      handleMessage
    }
  }

}