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

package signalcollect.implementations.messaging

import org.junit.Test
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import scala.collection.mutable.{ IndexedSeq, ArrayBuffer, ListBuffer }
import org.specs2.mock.Mockito
import signalcollect.interfaces.Worker
import signalcollect.interfaces.MessageRecipient

@RunWith(classOf[JUnitRunner])
class MessageBusSpec extends SpecificationWithJUnit with Mockito {

  "DefaultMessageBus" should {
    val mockCoordinator = mock[MessageRecipient[Any]]
    val mockWorker0 = mock[Worker]
    val mockWorker1 = mock[Worker]
    val mockLogger = mock[MessageRecipient[Any]]
    val defaultMessageBus = new DefaultMessageBus[Any, Any]
    defaultMessageBus.registerCoordinator(mockCoordinator)
    defaultMessageBus.registerWorker(0, mockWorker0)
    defaultMessageBus.registerWorker(1, mockWorker1)
    defaultMessageBus.registerLogger(mockLogger)

    "deliver message for id to worker0" in {
      // the id string's hash code mod 2 equals 0
      defaultMessageBus.sendToWorkerForId("someMessage1", "this.hashCode.abs % 2 == 0")
      there was one(mockWorker0).send("someMessage1")
    }

    "deliver message for id to worker1" in {
      // the id string's hash code mod 2 equals 1
      defaultMessageBus.sendToWorkerForId("someMessage1", "this.hashCode.abs % 2 == 1")
      there was one(mockWorker1).send("someMessage1")
    }

    "deliver message for id hash to worker0" in {
      defaultMessageBus.sendToWorkerForIdHash("someMessage2", 0)
      there was one(mockWorker0).send("someMessage2")
    }

    "deliver message for id hash to worker1" in {
      defaultMessageBus.sendToWorkerForIdHash("someMessage2", 1)
      there was one(mockWorker1).send("someMessage2")
    }
    
    "deliver message to all workers" in {
      defaultMessageBus.sendToWorkers("someMessageForAll")
      there was one(mockWorker0).send("someMessageForAll")
      there was one(mockWorker1).send("someMessageForAll")
    }

    "deliver message to coordinator" in {
      defaultMessageBus.sendToCoordinator("someMessage")
      there was one(mockCoordinator).send("someMessage")
    }

    "deliver message to logger" in {
      defaultMessageBus.sendToLogger("someMessage")
      there was one(mockLogger).send("someMessage")
    }

  }

}