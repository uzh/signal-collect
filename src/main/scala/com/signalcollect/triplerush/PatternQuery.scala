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

import java.util.concurrent.atomic.AtomicInteger

object QueryIds {
  private val maxFullQueryId = new AtomicInteger
  private val minSamplingQueryId = new AtomicInteger
  def nextFullQueryId = maxFullQueryId.incrementAndGet
  def nextSamplingQueryId = minSamplingQueryId.decrementAndGet
}

case class PatternQuery(
  queryId: Int,
  unmatched: Array[TriplePattern],
  //  variables: Array[Int],
  bindings: Array[Int],
  tickets: Long = Long.MaxValue, // normal queries have a lot of tickets
  isComplete: Boolean = true // set to false as soon as there are not enough tickets to follow all edges
  ) {

  //  implicit def arrayToBindings(array: Array[(Int, Int)]) = Bindings(array)
  implicit def int2expression(i: Int) = Expression(i)

  def isSamplingQuery = queryId < 0

  override def toString = {
    unmatched.mkString("\n") + bindings.toString
  }

  def getBindings: Array[(Int, Int)] = {
    ((-1 to -bindings.length by -1).zip(bindings)).toArray
  }

  /**
   * Assumption: tp has all constants.
   */
  def bind(tp: TriplePattern): PatternQuery = {
    if (unmatched.isEmpty) {
      throw new Exception("Why bind when this query is done? Optimize this code.")
    }
    val patternToMatch = unmatched.head
    if (patternToMatch.s == tp.s) { // Subject is compatible constant. No binding necessary. 
      if (patternToMatch.p == tp.p) { // Predicate is compatible constant. No binding necessary. 
        if (patternToMatch.o == tp.o) { // Object is compatible constant. No binding necessary. 
          return PatternQuery(queryId, unmatched.slice(1, unmatched.length), bindings, tickets, isComplete)
        }
        return bindObject(patternToMatch, tp)
      }
      return bindPredicate(patternToMatch, tp)
    }
    return bindSubject(patternToMatch, tp)
  }

  private def bindSubject(
    patternToMatch: TriplePattern,
    tp: TriplePattern,
    newUnmatched: Array[TriplePattern] = unmatched.slice(1, unmatched.length),
    newBindings: Array[Int] = bindings.clone): PatternQuery = {
    // Subject is conflicting constant. No binding possible.
    if (!patternToMatch.s.isVariable) return failedBinding

    // Subject is a variable that needs to be bound to the constant in the triple pattern. 
    // Bind value to variable.
    val variable = patternToMatch.s
    val boundValue = tp.s

    if (isBindingIncompatible(patternToMatch.p, tp.p, variable, boundValue) || isBindingIncompatible(patternToMatch.o, tp.o, variable, boundValue)) return failedBinding

    // No conflicts, we bind the value to the variable.
    val variableIndex = -(variable + 1)
    newBindings(variableIndex) = boundValue

    updateUnmatchedPatterns(variable, boundValue, newUnmatched)

    bindPredicate(patternToMatch, tp, newUnmatched, newBindings)
  }

  private def bindPredicate(
    patternToMatch: TriplePattern,
    tp: TriplePattern,
    newUnmatched: Array[TriplePattern] = unmatched.slice(1, unmatched.length),
    newBindings: Array[Int] = bindings.clone): PatternQuery = {

    if (patternToMatch.p == tp.p) { // Predicate is compatible constant. No binding necessary. 
      return bindObject(patternToMatch, tp, newUnmatched, newBindings)
    }

    // Predicate is conflicting constant. No binding possible.
    if (!patternToMatch.p.isVariable) return failedBinding

    // Predicate is a variable that needs to be bound to the constant in the triple pattern. 
    // Bind value to variable.
    val variable = patternToMatch.p
    val boundValue = tp.p

    if (isBindingIncompatible(patternToMatch.o, tp.o, variable, boundValue)) return failedBinding

    // No conflicts, we bind the value to the variable.
    val variableIndex = -(variable + 1)
    newBindings(variableIndex) = boundValue

    updateUnmatchedPatterns(variable, boundValue, newUnmatched)

    bindObject(patternToMatch, tp, newUnmatched, newBindings)
  }

  private def bindObject(
    patternToMatch: TriplePattern,
    tp: TriplePattern,
    newUnmatched: Array[TriplePattern] = unmatched.slice(1, unmatched.length),
    newBindings: Array[Int] = bindings.clone): PatternQuery = {

    if (patternToMatch.o == tp.o) { // Object is compatible constant. No binding necessary. 
      return PatternQuery(queryId, newUnmatched, newBindings, tickets, isComplete)
    }

    // Object is conflicting constant. No binding possible.
    if (!patternToMatch.o.isVariable) return failedBinding

    // Object is a variable that needs to be bound to the constant in the triple pattern. 
    // Bind value to variable.
    val variable = patternToMatch.o
    val boundValue = tp.o

    // No conflicts, we bind the value to the variable.
    val variableIndex = -(variable + 1)
    newBindings(variableIndex) = boundValue

    updateUnmatchedPatterns(variable, boundValue, newUnmatched)

    PatternQuery(queryId, newUnmatched, newBindings, tickets, isComplete)
  }

  // If the variable appears multiple times in the same pattern, then all the bindings have to be compatible.  
  private def isBindingIncompatible(otherAttribute: Int, tpAttribute: Int, variable: Int, boundValue: Int) = (otherAttribute == variable && tpAttribute != boundValue)

  private def failedBinding = null.asInstanceOf[PatternQuery]

  // Updates an attribute with a new binding.
  private def updatedAttribute(attribute: Int, variable: Int, boundValue: Int) = if (attribute == variable) boundValue else attribute

  // Update all the other patterns with this new binding.
  private def updateUnmatchedPatterns(variable: Int, boundValue: Int, newUnmatched: Array[TriplePattern]) {
    var i = 0
    while (i < newUnmatched.length) {
      val oldPattern = newUnmatched(i)
      val newS = updatedAttribute(oldPattern.s, variable, boundValue)
      val newP = updatedAttribute(oldPattern.p, variable, boundValue)
      val newO = updatedAttribute(oldPattern.o, variable, boundValue)
      newUnmatched(i) = TriplePattern(newS, newP, newO)
      i += 1
    }
  }

  def withUnmatchedPatterns(newUnmatched: Array[TriplePattern]) = {
    copy(unmatched = newUnmatched)
  }

  def withTickets(numberOfTickets: Long, complete: Boolean = true) = {
    copy(tickets = numberOfTickets, isComplete = complete)
  }
}