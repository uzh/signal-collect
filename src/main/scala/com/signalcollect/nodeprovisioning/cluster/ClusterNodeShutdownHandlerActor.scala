/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 iHealth Technologies
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

package com.signalcollect.nodeprovisioning.cluster

import akka.actor.{ Actor, ActorLogging }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ InitialStateAsEvents, MemberExited }

class ClusterNodeShutdownHandlerActor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberExited])
  }

  override def receive: Receive = {
    case MemberExited(m) =>
      log.info(s"Cluster node received member exited message for {} and is shutting down as well.", m)
      cluster.down(cluster.selfAddress)
      context.system.shutdown()
  }

}
