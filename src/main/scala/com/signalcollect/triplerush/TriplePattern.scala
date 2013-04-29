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
import com.signalcollect.triplerush.Expression._
import scala.Option.option2Iterable

/**
 * Pattern of 3 expressions (subject, predicate object).
 * They are represented as Int, but can be converted to Expression implicitly and for free (value class).
 */
case class TriplePattern(s: Int, p: Int, o: Int) {

  override def toString = {
    s"(${s.toString},${p.toString},${o.toString})"
  }

  def variables: List[Int] = {
    val sOpt = if (s.isVariable) Some(s) else None
    val pOpt = if (p.isVariable) Some(p) else None
    val oOpt = if (o.isVariable) Some(o) else None
    (sOpt :: pOpt :: oOpt :: Nil).flatten
  }

  def contains(expression: Int): Boolean = {
    if (s == expression) {
      return true
    } else if (p == expression) {
      return true
    } else if (o == expression) {
      return true
    } else {
      return false
    }
  }

  def childPatternRecipe: Int => TriplePattern = {
    this match {
      case TriplePattern(*, *, *) =>
        TriplePattern(_, *, *)
      case TriplePattern(*, p, *) =>
        TriplePattern(*, p, _)
      case TriplePattern(*, *, o) =>
        TriplePattern(_, *, o)
      case TriplePattern(s, *, *) =>
        TriplePattern(s, _, *)
      case TriplePattern(*, p, o) =>
        TriplePattern(_, p, o)
      case TriplePattern(s, *, o) =>
        TriplePattern(s, _, o)
      case TriplePattern(s, p, *) =>
        TriplePattern(s, p, _)
    }
  }

  def parentIdDelta(parentPattern: TriplePattern): Int = {
    if (parentPattern.s.isWildcard && !s.isWildcard) {
      s
    } else if (parentPattern.p.isWildcard && !p.isWildcard) {
      p
    } else if (parentPattern.o.isWildcard && !o.isWildcard) {
      o
    } else {
      throw new Exception(s"$parentPattern is not a parent pattern of ")
    }
  }

  def parentPatterns: List[TriplePattern] = {
    this match {
      case TriplePattern(*, *, *) =>
        List()
      case TriplePattern(s, *, *) =>
        List(TriplePattern(*, *, *))
      case TriplePattern(*, p, *) =>
        List()
      case TriplePattern(*, *, o) =>
        List()
      case TriplePattern(*, p, o) =>
        List(TriplePattern(*, p, *))
      case TriplePattern(s, *, o) =>
        List(TriplePattern(*, *, o))
      case TriplePattern(s, p, *) =>
        List(TriplePattern(s, *, *))
      case TriplePattern(s, p, o) =>
        List(TriplePattern(*, p, o), TriplePattern(s, *, o), TriplePattern(s, p, *))
    }
  }

  def isFullyBound: Boolean = {
    s.isConstant && p.isConstant && o.isConstant
  }

  /**
   * Returns the id of the index/triple vertex to which this pattern should be routed.
   * Any variables (<0) should be converted to "unbound", which is represented by a wildcard.
   */
  def routingAddress = {
    if (isFullyBound) {
      //Evenly load balance over all 3 index vertices for this triple.
      val routingIndex = hashCode % 3 // 0 = subject, 1 = predicate, 2 = object
      if (routingIndex == 0) {
        TriplePattern(*, p.toRoutingAddress, o.toRoutingAddress)
      } else if (routingIndex == 1) {
        TriplePattern(s.toRoutingAddress, *, o.toRoutingAddress)
      } else {
        TriplePattern(s.toRoutingAddress, p.toRoutingAddress, *)
      }
    } else {
      TriplePattern(s.toRoutingAddress, p.toRoutingAddress, o.toRoutingAddress)
    }
  }
//
//  /**
//   * Applies bindings to this pattern.
//   */
//  def applyBindings(bindings: Bindings): TriplePattern = {
//    TriplePattern(s.applyBindings(bindings), p.applyBindings(bindings), o.applyBindings(bindings))
//  }
//
//  /**
//   * Returns if this pattern can be bound to a triple.
//   * If it can be bound, then the necessary bindings are returned.
//   */
//  def bindingsFor(tp: TriplePattern): Option[Bindings] = {
//    val sBindings = s.bindTo(tp.s)
//    if (sBindings.isDefined) {
//      val pBindings = p.bindTo(tp.p)
//      if (pBindings.isDefined && pBindings.get.isCompatible(sBindings.get)) {
//        val spBindings = sBindings.get.merge(pBindings.get)
//        val oBindings = o.bindTo(tp.o)
//        if (oBindings.isDefined && oBindings.get.isCompatible(spBindings)) {
//          val spoBindings = spBindings.merge(oBindings.get)
//          Some(spoBindings)
//        } else {
//          None
//        }
//      } else {
//        None
//      }
//    } else {
//      None
//    }
//  }
}