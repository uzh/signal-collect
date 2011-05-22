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

import signalcollect.api.Factory._
import signalcollect.api.Factory
import signalcollect.interfaces._
import signalcollect.interfaces.ComputationStatistics
import signalcollect.implementations.logging.SeparateThreadLogger
import java.util.concurrent.BlockingQueue

class SynchronousCoordinator(
  numberOfWorkers: Int,
  workerFactory: WorkerFactory,
  messageBusFactory: MessageBusFactory,
  storageFactory: StorageFactory,
  logger: Option[MessageRecipient[Any]],
  signalThreshold: Double,
  collectThreshold: Double,
  messageInboxFactory: QueueFactory = Factory.Queue.Default)
  extends AbstractCoordinator(
    numberOfWorkers,
    workerFactory,
    messageBusFactory,
    storageFactory,
    logger,
    signalThreshold,
    collectThreshold,
    messageInboxFactory) {

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

  override def setStepsLimit(l: Int) {
    stepsLimit = 0
  }
  
}