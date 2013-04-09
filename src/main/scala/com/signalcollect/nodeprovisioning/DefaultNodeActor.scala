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
import com.signalcollect.configuration.EventBased
import com.signalcollect.configuration.Pinned
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import com.signalcollect.interfaces.Request
import com.signalcollect.interfaces.WorkerActor
import com.signalcollect.interfaces.NodeActor
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.MessageBus
import scala.reflect.ClassTag
import akka.japi.Creator
import com.signalcollect.interfaces.NodeReady
import com.signalcollect.interfaces.NodeStatus
import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import akka.actor.ReceiveTimeout
import com.signalcollect.interfaces.Heartbeat

/**
 * Creator in separate class to prevent excessive closure-capture of the TorqueNodeProvisioner class (Error[java.io.NotSerializableException TorqueNodeProvisioner])
 */
case class NodeActorCreator(
    nodeId: Int,
    nodeProvisionerAddress: Option[String]) extends Creator[NodeActor] {
  def create: NodeActor = new DefaultNodeActor(
    nodeId,
    nodeProvisionerAddress)
}

/**
 * Class that controls a node on which Signal/Collect workers run.
 */
class DefaultNodeActor(
    val nodeId: Int,
    val nodeProvisionerAddress: Option[String] // Specify if the worker should report when it is ready.
    ) extends NodeActor {

  var statusReportingInterval = 10 // Report every 10 milliseconds.

  var receivedMessagesCounter = 0

  // To keep track of sent messages before the message bus is initialized.
  var initializationMessageCounter = 0

  // To keep track of the workers this node is responsible for.
  var workers: List[ActorRef] = List[ActorRef]()

  def setStatusReportingInterval(interval: Int) {
    this.statusReportingInterval = interval
  }

  /**
   * Timeout for Akka actor idling
   */
  context.setReceiveTimeout(statusReportingInterval milliseconds)

  def receive = {
    /**
     * ReceiveTimeout message only gets sent after Akka actor mailbox has been empty for "receiveTimeout" milliseconds
     */
    case ReceiveTimeout =>
      sendStatusToCoordinator
    case Heartbeat(maySignal) =>
      sendStatusToCoordinator
    case Request(command, reply) =>
      receivedMessagesCounter += 1
      val result = command.asInstanceOf[Node => Any](this)
      if (reply) {
        if (result == null) { // Netty does not like null messages: org.jboss.netty.channel.socket.nio.NioWorker - WARNING: Unexpected exception in the selector loop. - java.lang.NullPointerException 
          if (isInitialized) {
            // MessageBus will take care of counting the replies.
            messageBus.sendToActor(sender, None)
          } else {
            // We need to manually keep track of these sent messages.
            initializationMessageCounter += 1
            sender ! None
          }
        } else {
          if (isInitialized) {
            // MessageBus will take care of counting the replies.
            messageBus.sendToActor(sender, result)
          } else {
            // We need to manually keep track of these sent messages.
            initializationMessageCounter += 1
            sender ! result
          }
        }
      }
    case other =>
      println("Received unexpected message from " + sender + ": " + other)
  }

  var messageBus: MessageBus[_, _] = _

  var nodeProvisioner: ActorRef = _

  def initializeMessageBus(numberOfWorkers: Int, numberOfNodes: Int, messageBusFactory: MessageBusFactory) {
    messageBus = messageBusFactory.createInstance(numberOfWorkers, numberOfNodes)
  }

  protected var lastStatusUpdate = System.currentTimeMillis

  protected def getNodeStatus: NodeStatus = {
    NodeStatus(
      nodeId = nodeId,
      messagesSent = messageBus.messagesSent + initializationMessageCounter + 1, // +1 to account for the status message itself.
      messagesReceived = receivedMessagesCounter)
  }

  protected def sendStatusToCoordinator {
    val currentTime = System.currentTimeMillis
    if (isInitialized && currentTime - lastStatusUpdate > statusReportingInterval) {
      lastStatusUpdate = currentTime
      val status = getNodeStatus
      messageBus.sendToCoordinator(status)
    }
  }

  def isInitialized = messageBus != null && messageBus.isInitialized

  def createWorker(workerId: Int, dispatcher: AkkaDispatcher, creator: () => WorkerActor[_, _]): String = {
    val workerName = "Worker" + workerId
    dispatcher match {
      case EventBased =>
        val worker = context.system.actorOf(Props[WorkerActor[_, _]].withCreator(creator()), name = workerName)
        workers = worker :: workers
        AkkaHelper.getRemoteAddress(worker, context.system)
      case Pinned =>
        val worker = context.system.actorOf(Props[WorkerActor[_, _]].withCreator(creator()).withDispatcher("akka.actor.pinned-dispatcher"), name = workerName)
        workers = worker :: workers
        AkkaHelper.getRemoteAddress(worker, context.system)
    }
  }

  def numberOfCores = Runtime.getRuntime.availableProcessors

  override def preStart() = {
    if (nodeProvisionerAddress.isDefined) {
      nodeProvisioner = context.actorFor(nodeProvisionerAddress.get)
      nodeProvisioner ! NodeReady
    }
  }

  def shutdown = context.system.shutdown

  def registerWorker(workerId: Int, worker: ActorRef) {
    messageBus.registerWorker(workerId, worker)
  }

  def registerNode(nodeId: Int, node: ActorRef) {
    messageBus.registerNode(nodeId, node)
  }

  def registerCoordinator(coordinator: ActorRef) {
    messageBus.registerCoordinator(coordinator)
  }

  def registerLogger(logger: ActorRef) {
    messageBus.registerLogger(logger)
  }

}