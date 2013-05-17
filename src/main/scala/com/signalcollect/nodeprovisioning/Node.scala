/*
 *  @author Philip Stutz
 *  @author Thomas Keller
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

package com.signalcollect.nodeprovisioning

import com.signalcollect.configuration.AkkaDispatcher
import com.signalcollect.interfaces.WorkerActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.ExtendedActorSystem
import com.signalcollect.interfaces.MessageBusFactory

trait Node {
  def createWorker(workerId: Int, dispatcher: AkkaDispatcher, creator: () => WorkerActor[_, _]): String // string = remote actor address
  def initializeMessageBus(numberOfWorkers: Int, numberOfNodes: Int, messageBusFactory: MessageBusFactory)
  def setStatusReportingInterval(statusReportingInterval: Int)
  def numberOfCores: Int
  def shutdown
}

object AkkaHelper {
  def getRemoteAddress(actorRef: ActorRef, system: ActorSystem): String = {
    val dummyDestination = Address("akka", "sys", "someHost", 42) // see http://groups.google.com/group/akka-user/browse_thread/thread/9448d8f628d38cc0
    val akkaSystemAddress = system.asInstanceOf[ExtendedActorSystem].provider.getExternalAddressFor(dummyDestination)
    val nodeProvisionerAddress = actorRef.path.toStringWithAddress(akkaSystemAddress.get)
    nodeProvisionerAddress.toString
  }
}