/*
 *  @author Philip Stutz
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

package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode

class Company(id: String, coreValue: Double) extends DataGraphVertex(id, coreValue) {
  type Signal = Double
  def collect = coreValue + signals.sum
}

class OwnedBy(targetId: String, percentage: Double) extends DefaultEdge(targetId) {
  type Source = Company
  def signal = source.state * percentage
}

/**
 * Example computation of the total value of companies that have mutual stakes in each other.
 *
 * Renault currently has a 44.4 percent stake in Nissan, and Nissan holds a 15 percent stake in Renault.
 * Source: http://en.wikipedia.org/wiki/Renault-Nissan_Alliance.
 */
object CaompanyValuation extends App {
  val graph = GraphBuilder.withConsole(true).build
  graph.addVertex(new Company("Nissan", coreValue = 37)) //market cap: ~ USD 42 billion
  graph.addVertex(new Company("Renault", coreValue = 14)) // market cap: ~ USD 33 billion
  graph.addEdge("Nissan", new OwnedBy("Renault", 0.444))
  graph.addEdge("Renault", new OwnedBy("Nissan", 0.15))
  graph.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive))
  graph.foreachVertex(println(_))
  graph.shutdown
}
