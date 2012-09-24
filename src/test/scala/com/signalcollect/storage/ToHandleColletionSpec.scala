/*
 *  @author Daniel Strebel
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

package com.signalcollect.storage

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import com.signalcollect.messaging.DefaultMessageBus
import java.io.File

@RunWith(classOf[JUnitRunner])
class ToHandleColletionSpec extends SpecificationWithJUnit with Mockito {

  sequential
  
  "InMemoryVertexIdSet" should {
    val storage = mock[Storage]
    val toSignal = new InMemoryVertexIdSet(storage)
    val fakeVertexIds = List(1, 2, 3, 4)

    "hold all ids inserted" in {
      for (id <- fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.size === fakeVertexIds.size
    }

    "contain no duplicates" in {
      for (id <- fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.add(fakeVertexIds(1))
      toSignal.size === fakeVertexIds.size
    }

    "call foreach on each id without removing them" in {
      for (id <- fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.foreach(id => id.toString(), false)
      toSignal.size === fakeVertexIds.size
    }

    "call foreach on each id and removing them" in {
      for (id <- fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.foreach(id => id.toString(), true)
      toSignal.size === 0
    }
  }

  "DefaultVertexSignalBuffer" should {
    val storage = mock[Storage]
    val toCollect = new InMemoryVertexSignalBuffer
    val signalMessage1a = new SignalMessage(1, EdgeId(0, 1))
    val signalMessage1b = new SignalMessage(1, EdgeId(2, 1))
    val signalMessage2 = new SignalMessage(1, EdgeId(1, 2))
    val signalMessage3 = new SignalMessage(1, EdgeId(1, 3))

    val allSignalMessages = List(signalMessage1a, signalMessage2, signalMessage3)

    "hold buffered signals" in {
      for (signalMessage <- allSignalMessages) {
        toCollect.addSignal(signalMessage)
      }
      toCollect.size === allSignalMessages.size
    }

    "collect signals for the same target vertex" in {
      for (signalMessage <- allSignalMessages) {
        toCollect.addSignal(signalMessage)
      }
      toCollect.addSignal(signalMessage1b)
      toCollect.size === allSignalMessages.size
    }

    "call foreach on each id without removing them" in {
      for (signalMessage <- allSignalMessages) {
        toCollect.addSignal(signalMessage)
      }
      toCollect.addSignal(signalMessage1b)

      toCollect.foreach((id, signals) => id.toString(), false)
      toCollect.size === allSignalMessages.size
    }

    "call foreach on each id and removing them" in {
      for (signalMessage <- allSignalMessages) {
        toCollect.addSignal(signalMessage)
      }
      toCollect.addSignal(signalMessage1b)

      toCollect.foreach((id, signals) => id.toString(), true)
      toCollect.size === 0
    }
  }
}