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

import com.typesafe.config.ConfigFactory
import interfaces.{ WorkerApi, Request }
import messaging.AkkaProxy
import akka.actor.{ Props, ActorSystem, Actor }
import org.scalatest.Matchers
import org.scalatest.FlatSpec

class AkkaProxySpec extends FlatSpec with Matchers {

  "AkkaProxy" should "invoke blocking methods" in {

    trait Sleeper extends Actor {
      def sleep(milliSeconds: Int) = {
        Thread.sleep(milliSeconds)
      }

      def receive = {
        case Request(command, reply, incrementor) =>
          try {
            val result = command.asInstanceOf[Sleeper => Any](this)
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
    val sleeper = system.actorOf(Props(new Object with Sleeper), name = "sleeper")
    val sleeperProxy = AkkaProxy.newInstance[Sleeper](sleeper)

    val expectedSleepTime = 300

    val sleepStart = System.currentTimeMillis()
    sleeperProxy.sleep(expectedSleepTime)
    val sleepStop = System.currentTimeMillis()
    val measuredSleepTime: Double = sleepStop - sleepStart

    measuredSleepTime should (be > expectedSleepTime * 0.9 and be < expectedSleepTime * 1.1)
    system.shutdown()
  }

}
