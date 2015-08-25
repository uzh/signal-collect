/*
 *  @author Philip Stutz
 *  @author Bharath Kumar
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

import com.signalcollect.node.DefaultNodeActor

import akka.actor.{ Actor, ActorLogging, ActorRef, Address, Deploy, Props, actorRef2Scala }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ InitialStateAsEvents, MemberUp }
import akka.remote.RemoteScope

case object RetrieveNodeActors

class ClusterNodeProvisionerActor(
    idleDetectionPropagationDelayInMilliseconds: Int,
    actorNamePrefix: String,
    numberOfNodes: Int) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  val masterNodeActor = startNodeActor(cluster.selfAddress, 0)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  private[this] var nodeActorArrayRequestor: Option[ActorRef] = None

  private[this] var nodeActors: Map[Int, ActorRef] = Map(0 -> masterNodeActor)

  private[this] var nextNodeActorId = 1

  override def receive: Receive = {
    case RetrieveNodeActors =>
      assert(nodeActorArrayRequestor.isEmpty, "Cannot have more than one requestor for node actors.")
      nodeActorArrayRequestor = Some(sender)
      reportNodeActors
    case MemberUp(m) =>
      log.info(s"Cluster master received member up message for {}.", m)
      if (m.address != cluster.selfAddress) {
        val nodeActor = startNodeActor(m.address, nextNodeActorId)
        nodeActors += nextNodeActorId -> nodeActor
        nextNodeActorId += 1
        reportNodeActors
      }
  }

  def reportNodeActors(): Unit = {
    if (nodeActors.size == numberOfNodes && nodeActorArrayRequestor.isDefined) {
      assert((0 until numberOfNodes).toSet == nodeActors.keys.toSet)
      val nodeActorArray = new Array[ActorRef](numberOfNodes)
      nodeActors.foreach { case (k, v) => nodeActorArray(k) = v }
      nodeActorArrayRequestor.foreach { _ ! nodeActorArray }
      log.info(s"All expected {} nodes are up and the respective node actors were created.", numberOfNodes)
      context.stop(self)
    }
  }

  def startNodeActor(address: Address, nodeActorId: Int): ActorRef = {
    val nodeController = context.system.actorOf(
      Props(classOf[DefaultNodeActor[Long, Any]],
        actorNamePrefix,
        nodeActorId,
        numberOfNodes,
        None,
        idleDetectionPropagationDelayInMilliseconds,
        None).withDeploy(Deploy(scope = RemoteScope(address))),
      name = actorNamePrefix + "DefaultNodeActor" + nodeActorId.toString)
    nodeController
  }

}
