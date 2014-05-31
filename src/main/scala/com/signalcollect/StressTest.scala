///*
// *  @author Philip Stutz
// *
// *  Copyright 2014 University of Zurich
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *         http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// *
// */
//
//package com.signalcollect
//
//import org.scalatest.FlatSpec
//import org.scalatest.Matchers
//
//class CrazyLoadVertex(id: Int, initialState: Int) extends MemoryEfficientDataGraphVertex(id, initialState) {
//  type Signal = Int
//  type OutgoingSignalType = Int
//
//  def collect = state + 1
//
//  def computeSignal(targetId: Int) = state
//
//  def scoreSignal = {
//    if (lastSignalState != state && state < 1000) {
//      1
//    } else {
//      0
//    }
//  }
//
//  def scoreCollect = 1
//
//}
//
//class StressTest extends FlatSpec with Matchers {
//
//  "Signal/Collect" should "correctly work when the system is pushed to the limit" in {
//    val g = GraphBuilder.withMessageSerialization(true).withKryoRegistrations(List(
//      "com.signalcollect.interfaces.AddVertex",
//      "com.signalcollect.StateForwarderEdge",
//      "com.signalcollect.CrazyLoadVertex",
//      "com.signalcollect.util.IntHashMap",
//      "scala.reflect.ManifestFactory$$anon$5",
//      "scala.runtime.Nothing$",
//      "com.signalcollect.MemoryEfficientSplayIntSet",
//      "com.signalcollect.util.SplayNode")).build
//    for (id <- 1 until 1000) {
//      g.addVertex(new CrazyLoadVertex(id, 1))
//      for (to <- 1 until 1000) {
//        g.addEdge(id, new StateForwarderEdge(to))
//      }
//    }
//    g.execute
//    g.shutdown
//  }
//
//}
