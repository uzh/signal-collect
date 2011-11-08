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

package com.signalcollect.implementations.storage

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import com.signalcollect.interfaces._
import com.signalcollect.implementations.messaging.DefaultMessageBus
import java.io.File
import com.signalcollect.EdgeId

@RunWith(classOf[JUnitRunner])
class ToHandleColletionSpec extends SpecificationWithJUnit with Mockito {

  "InMemoryVertexIdSet" should {
    val storage = mock[Storage]
    val toSignal = new InMemoryVertexIdSet(storage)
    val fakeVertexIds = List(1,2,3,4)
    
    "hold all ids inserted" in {
      for(id<-fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.size===fakeVertexIds.size
    }
    
    "contain no duplicates" in {
      for(id<-fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.add(fakeVertexIds(1))
      toSignal.size===fakeVertexIds.size
    }
    
    "call foreach on each id without removing them" in {
      for(id<-fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.foreach(id => id.toString(), false)
      toSignal.size===fakeVertexIds.size
    }
    
    "call foreach on each id and removing them" in {
      for(id<-fakeVertexIds) {
        toSignal.add(id)
      }
      toSignal.foreach(id => id.toString(), true)
      toSignal.size===0
    }
  }
  
  "DefaultVertexSignalBuffer" should {
    val storage = mock[Storage]
    val toCollect = new InMemoryVertexSignalBuffer
    val fakeSignal1a = mock[SignalMessage[Int,Int,Int]]
    		
    val fakeEdgeId1a = mock[EdgeId[Int,Int]]
    fakeSignal1a.edgeId returns fakeEdgeId1a
    fakeEdgeId1a.targetId returns 1
    
    val fakeSignal1b = mock[SignalMessage[Int,Int,Int]]
    val fakeEdgeId1b = mock[EdgeId[Int,Int]]
    fakeSignal1b.edgeId returns fakeEdgeId1b
    fakeEdgeId1b.targetId returns 1
    
    
    val fakeSignal2 = mock[SignalMessage[Int,Int,Int]]
    val fakeEdgeId2 = mock[EdgeId[Int,Int]]
    fakeSignal2.edgeId returns fakeEdgeId2
    fakeEdgeId2.targetId returns 2
    
    val fakeSignal3 = mock[SignalMessage[Int,Int,Int]]
    val fakeEdgeId3 = mock[EdgeId[Int,Int]]
    fakeSignal3.edgeId returns fakeEdgeId3
    fakeEdgeId3.targetId returns 3
    
    val allSignals = List(fakeSignal1a, fakeSignal2, fakeSignal3)
    
    "hold buffered signals" in {
       for(signal<-allSignals) {
        toCollect.addSignal(signal)
      }
      toCollect.size === allSignals.size
    }
    
    "collect signals for the same target vertex" in {
      for(signal<-allSignals) {
        toCollect.addSignal(signal)
      }
      toCollect.addSignal(fakeSignal1b)
      toCollect.size === allSignals.size
    }
    
    "call foreach on each id without removing them" in {
      for(signal <- allSignals) {
        toCollect.addSignal(signal)
      }
      toCollect.addSignal(fakeSignal1b)

      toCollect.foreach((id, signals) => id.toString(), false)
      toCollect.size === allSignals.size
    }
    
    "call foreach on each id and removing them" in {
      for(signal <- allSignals) {
        toCollect.addSignal(signal)
      }
      toCollect.addSignal(fakeSignal1b)

      toCollect.foreach((id, signals) => id.toString(), true)
      toCollect.size === 0
    }
  }
}