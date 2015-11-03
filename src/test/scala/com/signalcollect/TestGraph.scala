/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 Cotiviti
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

package com.signalcollect

import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

import scala.annotation.tailrec
import scala.reflect.runtime.universe
import scala.util.{ Failure, Success, Try }

import org.scalatest.fixture.NoArg

import com.signalcollect.configuration.Akka

import akka.actor.ActorSystem

object TestGraph {

  def instantiateUniqueGraph(): Graph[Any, Any] = {
    instantiateUniqueGraphBuilder().build
  }

  def instantiateUniqueActorSystem(): ActorSystem = {
    val port = freePort
    val akkaConfig = Akka.config(
      serializeMessages = None,
      loggingLevel = None,
      kryoRegistrations = GraphBuilder.config.kryoRegistrations,
      kryoInitializer = None,
      port = Some(port),
      seedPort = Some(port))
    val actorSystemName = "SignalCollectTestSystem"
    ActorSystem(actorSystemName, akkaConfig)
  }

  def instantiateUniqueGraphBuilder(): GraphBuilder[Any, Any] = {
    new GraphBuilder[Any, Any]()
      .withActorSystem(instantiateUniqueActorSystem())
  }

  private[this] val portUsageTracker = new AtomicInteger(2500)

  def freePort: Int = {
    @tailrec def attemptToBindAPort(failuresSoFar: Int): Int = {
      val checkedPort = portUsageTracker.incrementAndGet
      val socketTry = Try(new ServerSocket(checkedPort))
      socketTry match {
        case Success(s) =>
          s.close()
          checkedPort
        case Failure(f) =>
          if (failuresSoFar > 10) {
            throw f
          } else {
            attemptToBindAPort(failuresSoFar + 1)
          }
      }
    }
    attemptToBindAPort(0)
  }

}

class TestGraph(val g: Graph[Any, Any]) extends NoArg {

  lazy implicit val system = g.system

  def this() = this(TestGraph.instantiateUniqueGraph())

  def shutdown(): Unit = {
    g.shutdown
  }

  override def apply(): Unit = {
    try {
      super.apply()
    } finally {
      shutdown()
    }
  }

}
