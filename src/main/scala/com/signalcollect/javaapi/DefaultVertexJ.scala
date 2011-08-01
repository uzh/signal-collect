///*
// *  @author Philip Stutz
// *  
// *  Copyright 2011 University of Zurich
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
//package com.signalcollect.javaapi
//
//import com.signalcollect.implementations.graph._
//import scala.collection.JavaConversions._
//import java.lang.Iterable
//import com.signalcollect.interfaces.MessageBus
//
//class DefaultVertexJ[IdType, StateType](
//  val id: IdType,
//  initialState: StateType)
//  extends AbstractVertex[IdType, StateType]
//  with UncollectedSignalsList[IdType, StateType]
//  with MostRecentSignalsMap
//  with SumOfOutWeights
//  with DefaultGraphApi {
//  
//  var messageBus: MessageBus[Any] = _
//  
//  /** This method gets called by the framework after the vertex has been fully initialized. */
//  override def afterInitialization(mb: MessageBus[Any]) {
//    messageBus = mb
//  } 
//
//  /** vertex state is initialized to initialState */
//  var state = initialState
//  
//  def signalsJ[G](filterClass: Class[G]): java.lang.Iterable[G] = new Iterable[G] {
//    def iterator = signals(filterClass).iterator
//  }
//  
//  def uncollectedSignalsJ[G](filterClass: Class[G]): java.lang.Iterable[G] = new Iterable[G] {
//    def iterator = uncollectedSignals(filterClass).iterator
//  }
//  
//  def collect: StateType = null.asInstanceOf[StateType]
//  
//}
// 
