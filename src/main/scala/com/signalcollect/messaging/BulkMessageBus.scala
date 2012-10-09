/*
 *  @author Philip Stutz
 *  
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.messaging

import com.signalcollect.interfaces.SignalMessage

class BulkMessageBus(numWorkers: Int, combine: (Any, Any) => Any)
    extends DefaultMessageBus(numWorkers) {

  val emptyMap = Map[Any, Any]()
  
  val outgoingMessages: Array[Map[Any, Any]] = new Array[Map[Any, Any]](numWorkers)
  for (i <- 0 until numberOfWorkers) {
    outgoingMessages(i) = emptyMap
  }

  override def flush {
    var workerId = 0
    while (workerId < numberOfWorkers) {
      val signalsForWorker = outgoingMessages(workerId)
      if (!signalsForWorker.isEmpty) {
        super.sendToWorker(workerId, signalsForWorker)
        outgoingMessages(workerId) = emptyMap
      }
      workerId += 1
    }
  }

  override def sendToWorker(workerId: Int, message: Any) {
    message match {
      case SignalMessage(signal, edgeId) =>
        val signalsForWorker = outgoingMessages(workerId)
        var signalForVertex = signalsForWorker.get(edgeId.targetId)
        signalForVertex match {
          case Some(oldSignal) => 
             outgoingMessages(workerId) = signalsForWorker + ((edgeId.targetId, signal.asInstanceOf[Float] + oldSignal.asInstanceOf[Float]))
          case None         =>
            outgoingMessages(workerId) = signalsForWorker + ((edgeId.targetId, signal))
        }
      case other =>
        super.sendToWorker(workerId, message)
    }
  }
}