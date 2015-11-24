/*
 *  @author Thomas Keller
 *
 *  Copyright 2013 University of Zurich
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

import java.util.concurrent.CountDownLatch

import org.scalatest.{ Finders, FlatSpec, Matchers }

import akka.actor.{ Actor, Props, actorRef2Scala }
import interfaces.Request
import messaging.AkkaProxy

class AkkaProxySpec extends FlatSpec with Matchers {

  "AkkaProxy" should "invoke blocking methods" in {

    object Counter {
      val latch = new CountDownLatch(3)
    }

    trait CommandExecutor extends Actor {

      def countDown(): Unit = {
        Counter.latch.countDown()
      }

      def receive = {
        case Request(command, reply, incrementor) =>
          try {
            val result = command.asInstanceOf[CommandExecutor => Any](this)
            if (reply) {
              if (result == null) {
                sender ! None
              } else {
                sender ! result
              }
            }
          } catch {
            case e: Exception =>
              throw e
          }
      }
    }

    val system = TestConfig.actorSystem("AkkaProxySpec")
    val executor = system.actorOf(Props(new Object with CommandExecutor))
    val proxy = AkkaProxy.newInstance[CommandExecutor](executor)
    proxy.countDown
    proxy.countDown
    proxy.countDown
    Counter.latch.await()
    system.terminate
  }

}
