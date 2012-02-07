/*
 *  @author Philip Stutz
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

package com.signalcollect.implementations.messaging

import akka.dispatch.Await
import com.signalcollect.interfaces._
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import com.signalcollect.interfaces._
import java.lang.reflect.Method
import akka.actor.ActorRef
import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import java.util.concurrent.TimeUnit
import akka.dispatch.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import com.signalcollect.interfaces.Request
import akka.pattern.ask
import com.signalcollect.interfaces.LogMessage

/**
 * Used to create proxies
 */
object AkkaProxy {

  def newInstance[T <: Any: Manifest](actor: ActorRef, loggerActor: ActorRef, sentMessagesCounter: AtomicInteger = new AtomicInteger(0), receivedMessagesCounter: AtomicInteger = new AtomicInteger(0), timeout: Timeout = Timeout(10000 milliseconds)): T = {
    val c = manifest[T].erasure
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new AkkaProxy(actor, loggerActor: ActorRef, sentMessagesCounter, receivedMessagesCounter, timeout)).asInstanceOf[T]
  }

}

/**
 *  Proxy that does RPC over Akka
 */
class AkkaProxy[ProxiedClass](actor: ActorRef, loggerActor: ActorRef, sentMessagesCounter: AtomicInteger, receivedMessagesCounter: AtomicInteger, timeout: Timeout) extends InvocationHandler {

  override def toString = "ProxyFor" + actor.toString

  implicit val t = timeout

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    val command = new Function1[ProxiedClass, AnyRef] {
      def apply(proxiedClass: ProxiedClass) = method.invoke(proxiedClass, arguments: _*)
      override def toString: String = method.getName + "(" + { if (arguments != null) { arguments.foldLeft("")(_ + _) + ")" } else { "" } }
    }
    try {
      val resultFuture: Future[Any] = actor ? Request(command, returnResult = true)
      sentMessagesCounter.incrementAndGet
      val result = Await.result(resultFuture, timeout.duration)
      receivedMessagesCounter.incrementAndGet
      result.asInstanceOf[AnyRef]
    } catch {
      case t: Throwable =>
        println("Exception in proxy, responsible method call: " + method.getName + "(" + { if (arguments != null) { arguments.foldLeft("")(_ + _) + ")" } else { "" } } + " Exception:\n" + t.getCause + "\n" + t)
        throw t
    }
  }

}