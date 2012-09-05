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

package com.signalcollect.messaging

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
import akka.pattern.ask
import com.signalcollect.interfaces.LogMessage
import com.signalcollect.serialization.DefaultSerializer

case class Request[ProxiedClass](command: ProxiedClass => Any, returnResult: Boolean = false)

/**
 * Used to create proxies
 */
object AkkaProxy {

  def newInstance[T <: Any: Manifest](actor: ActorRef, sentMessagesCounter: AtomicInteger = new AtomicInteger(0), receivedMessagesCounter: AtomicInteger = new AtomicInteger(0), timeout: Timeout = Timeout(7200 seconds)): T = {
    val c = manifest[T].erasure
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new AkkaProxy(actor, sentMessagesCounter, receivedMessagesCounter, timeout)).asInstanceOf[T]
  }

}

/**
 *  Proxy that does RPC over Akka
 */
class AkkaProxy[ProxiedClass](actor: ActorRef, sentMessagesCounter: AtomicInteger, receivedMessagesCounter: AtomicInteger, timeout: Timeout) extends InvocationHandler with Serializable {

  override def toString = "ProxyFor" + actor.toString

  implicit val t = timeout

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    val command = new Command[ProxiedClass](method.getDeclaringClass.getName, method.toString, arguments)
    try {
      val resultFuture: Future[Any] = actor ? Request(command, returnResult = true)
      sentMessagesCounter.incrementAndGet
      val result = Await.result(resultFuture, timeout.duration)
      receivedMessagesCounter.incrementAndGet
      result.asInstanceOf[AnyRef]
    } catch {
      case e: Exception =>
        println("Exception in proxy method `" + method.getName + "(" + { if (arguments != null) { arguments.foldLeft("")(_ + ", " + _) + ")`: " } else { "`: " } } + e + " from " + actor + " " + e.printStackTrace)
        throw e
    }
  }

}

case class Command[ParameterType](className: String, methodDescription: String, arguments: Array[Object]) extends Function1[ParameterType, AnyRef] {
  def apply(proxiedClass: ParameterType) = {
    val clazz = Class.forName(className)
    val methods = clazz.getMethods map (method => (method.toString, method)) toMap
    val method = methods(methodDescription)
    val result = method.invoke(proxiedClass, arguments: _*)
    result
  }

  override def toString: String = {
    className + "." + methodDescription + { if (arguments != null) { "(" + arguments.toList.mkString("(", ", ", ")") } else { "" } }
  }

}