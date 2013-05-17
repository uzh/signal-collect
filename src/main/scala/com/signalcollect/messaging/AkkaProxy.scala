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

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.Array.canBuildFrom
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.reflect.classTag
import com.signalcollect.interfaces.Request
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import com.signalcollect.interfaces.MessageBus

case class EmptyIncrementor() {
  def increment(mb: MessageBus[_, _]): Unit = {}
}

/**
 * Used to create proxies
 */
object AkkaProxy {

  def newInstanceWithIncrementor[T: ClassTag](
    actor: ActorRef,
    incrementor: MessageBus[_, _] => Unit,
    sentMessagesCounter: AtomicInteger = new AtomicInteger(0),
    receivedMessagesCounter: AtomicInteger = new AtomicInteger(0),
    timeout: Timeout = Timeout(Duration.create(2, TimeUnit.HOURS))): T = {
    val c = classTag[T].runtimeClass
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new AkkaProxy(actor,
        incrementor,
        sentMessagesCounter,
        receivedMessagesCounter,
        timeout)).asInstanceOf[T]
  }

  def newInstance[T: ClassTag](
    actor: ActorRef,
    sentMessagesCounter: AtomicInteger = new AtomicInteger(0),
    receivedMessagesCounter: AtomicInteger = new AtomicInteger(0),
    timeout: Timeout = Timeout(Duration.create(2, TimeUnit.HOURS))): T = {
    val c = classTag[T].runtimeClass
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new AkkaProxy(actor,
        EmptyIncrementor().increment _,
        sentMessagesCounter,
        receivedMessagesCounter,
        timeout)).asInstanceOf[T]
  }

}

/**
 *  Proxy that does RPC over Akka
 */
class AkkaProxy[ProxiedClass](
  actor: ActorRef,
  incrementor: MessageBus[_, _] => Unit,
  sentMessagesCounter: AtomicInteger,
  receivedMessagesCounter: AtomicInteger,
  timeout: Timeout,
  optimizeBlocking: Boolean = false) extends InvocationHandler with Serializable {

  override def toString = "ProxyFor" + actor.toString

  implicit val t = timeout

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    val command = new Command[ProxiedClass](method.getDeclaringClass.getName, method.toString, arguments)
    try {
      if (optimizeBlocking && method.getReturnType.equals(Void.TYPE)) {
        actor ! Request(command, returnResult = false, incrementor)
        sentMessagesCounter.incrementAndGet
        null.asInstanceOf[AnyRef] // Return type of method is void.
      } else {
        val resultFuture: Future[Any] = actor ? Request(command, returnResult = true, incrementor)
        sentMessagesCounter.incrementAndGet
        val result = Await.result(resultFuture, timeout.duration)
        receivedMessagesCounter.incrementAndGet
        result.asInstanceOf[AnyRef]
      }
    } catch {
      case e: Exception =>
        println("Exception in proxy method `" + method.getName + "(" + { if (arguments != null) { arguments.foldLeft("")(_ + ", " + _) + ")`: " } else { "`: " } } + e + " from " + actor + " " + e.printStackTrace)
        throw e
    }
  }

}

case class Command[ParameterType](className: String, methodDescription: String, arguments: Array[Object]) extends Function1[ParameterType, AnyRef] {
  def apply(proxiedClass: ParameterType) = {
    try {
      val clazz = Class.forName(className)
      val methods = clazz.getMethods map (method => (method.toString, method)) toMap
      val method = methods(methodDescription)
      val result = method.invoke(proxiedClass, arguments: _*)
      result
    } catch {
      case t: Throwable =>
        val argsString = {
          if (arguments != null) {
            arguments.toList.mkString(", ")
          } else {
            "null"
          }
        }
        println(s"Exception when trying to execute $methodDescription with argument(s) $argsString")
        println(t.getCause)
        println(t.getStackTraceString)
        println("Finished printing stack trace")
        throw t
    }
  }

  override def toString: String = {
    className + "." + methodDescription + { if (arguments != null) { "(" + arguments.toList.mkString("(", ", ", ")") } else { "" } }
  }

}
