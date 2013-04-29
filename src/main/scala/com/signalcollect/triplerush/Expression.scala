/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

package com.signalcollect.triplerush

object Expression {
  val * = 0
  implicit def int2Expression(i: Int) = Expression(i)
}

/**
 * Can represent either a constant, a variable or a wildcard.
 * Variables are represented by value < 0,
 * Constants are represented by value > 0.
 * Wildcards are represented by value == 0.
 */
case class Expression(value: Int) extends AnyVal {
  implicit def int2expression(i: Int) = Expression(i)

//  /**
//   * Applies a binding from bindings, if one applies.
//   */
//  @inline def applyBindings(bindings: Bindings): Int = {
//    if (isVariable && bindings.map.contains(value)) {
//      // This is a variable and there is a binding for it.
//      bindings.map(value)
//    } else {
//      // No change if this is not a variable or if there is no binding for it.
//      value
//    }
//  }
//  @inline def bindTo(constant: Int): Option[Bindings] = {
//    if (isVariable) {
//      // This is a variable, return the new binding.
//      Some(Bindings(Map(value -> constant.value)))
//    } else if (isConstant && value == constant.value) {
//      // Binding is compatible, but no new binding created.
//      Some(Bindings())
//    } else {
//      // Cannot bind this.
//      None
//    }
//  }
  @inline def isVariable = value < 0
  @inline def isConstant = value > 0
  @inline def isWildcard = value == 0
  @inline def toRoutingAddress = math.max(value, 0)
  @inline override def toString = Mapping.getString(value)
}