/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

package com.signalcollect.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

import scala.reflect.ClassTag
import scala.reflect.classTag

import com.signalcollect.messaging.Command

object Verifier {
  def create[T: ClassTag](reference: T, alternative: T): T = {
    val c = classTag[T].runtimeClass
    Proxy.newProxyInstance(
      c.getClassLoader,
      Array[Class[_]](c),
      new Verifier[T](reference, alternative)).asInstanceOf[T]
  }
}

/**
 * The verifier ensures that an alternative implementation of an
 * interface 'I' produces the same results as the reference implementation.
 * This class will behave like the reference implementation, but when the
 * alternative implementation behaves differently, it will throw an exception
 * and print the call history that led to the different behaviour.
 *
 * @note Create by calling Verifier.create(reference, alternative)
 * @note The return values have to be comparabl with a Scala equals comparison.
 */
class Verifier[I](referenceImplementation: I, alternativeImplementation: I) extends InvocationHandler {

  var commandHistory = List.empty[Command[I]]

  def invoke(proxy: Object, method: Method, arguments: Array[Object]) = {
    val command = new Command[I](method.getDeclaringClass.getName, method.toString, arguments)
    commandHistory = command :: commandHistory
    val referenceStateBefore = referenceImplementation.toString
    val alternativeStateBefore = alternativeImplementation.toString
    val referenceResult = command(referenceImplementation)
    val alternativeResult = command(alternativeImplementation)
    if (referenceResult != alternativeResult) {
      throw new Exception(s"Verifier error: Alternative implementation returned $alternativeResult, should have returned $referenceResult.\n" +
        s"Reference impl. before call: ${referenceStateBefore}\n" +
        s"Alternative impl. before call: ${alternativeStateBefore}\n" +
        s"Command history: ${commandHistory.mkString("\n")}")
    }
    referenceResult
  }

}
