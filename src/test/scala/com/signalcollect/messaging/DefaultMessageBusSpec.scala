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

package com.signalcollect.messaging

import org.junit.Test
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import scala.collection.mutable.{ IndexedSeq, ArrayBuffer, ListBuffer }
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import akka.actor.ActorRef

@RunWith(classOf[JUnitRunner])
class MessageBusSpec extends SpecificationWithJUnit with Mockito {

//  "DefaultMessageBus" should {
//    val mockCoordinator = mock[ActorRef]
//    val mockWorker0 = mock[ActorRef]
//    val mockWorker1 = mock[ActorRef]
//    val mockLogger = mock[ActorRef]
//    val defaultMessageBus = new DefaultMessageBus(2)
//    defaultMessageBus.registerCoordinator(mockCoordinator)
//    defaultMessageBus.registerWorker(0, mockWorker0)
//    defaultMessageBus.registerWorker(1, mockWorker1)
//
//    "deliver message for id to worker0" in {
//      // the id string's hash code mod 2 equals 0
//      defaultMessageBus.sendToWorkerForVertexId("someMessage1", "this.hashCode.abs % 2 == 0")
//      there was one(mockWorker0).receive("someMessage1")
//    }
//
//    "deliver message for id to worker1" in {
//      // the id string's hash code mod 2 equals 1
//      defaultMessageBus.sendToWorkerForVertexId("someMessage1", "this.hashCode.abs % 2 == 1")
//      there was one(mockWorker1).receive("someMessage1")
//    }
//
//    "deliver message for id hash to worker0" in {
//      defaultMessageBus.sendToWorkerForVertexIdHash("someMessage2", 0)
//      there was one(mockWorker0).receive("someMessage2")
//    }
//
//    "deliver message for id hash to worker1" in {
//      defaultMessageBus.sendToWorkerForVertexIdHash("someMessage2", 1)
//      there was one(mockWorker1).receive("someMessage2")
//    }
//    
//    "deliver message to all workers" in {
//      defaultMessageBus.sendToWorkers("someMessageForAll")
//      there was one(mockWorker0).receive("someMessageForAll")
//      there was one(mockWorker1).receive("someMessageForAll")
//    }
//
//    "deliver message to coordinator" in {
//      defaultMessageBus.sendToCoordinator("someMessage")
//      there was one(mockCoordinator).receive("someMessage")
//    }
//
//  }

}