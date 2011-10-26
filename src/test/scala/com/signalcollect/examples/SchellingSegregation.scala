/*
 *  @author Daniel Strebel
 *  
 *  Copyright 2011 University of Zurich
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

/**
 *  Represents an agent in a Schelling-Segregation simulation
 *
 *  @param id: the identifier of this vertex
 *  @param initialState: initial state of the agent
 *  @param equalityThreshold: minimum required percentage of neighbors with equal state as float value from 0 to 1.
 */
class SegregationAgent(id: Any, initialState: Int, equalityThreshold: Float) extends DataGraphVertex(id, initialState) {
  type Signal = Int
  var changedState: Boolean = false

  def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
    //val equalCount = mostRecentSignalMap.values.foldLeft(0)((b, otherState) => if (otherState == this.state) b + 1 else b)
    var equalCount = 0
    for (otherState <- mostRecentSignalMap.values) {
      if (otherState == this.state) {
        equalCount += 1
      }
    }
    val totalNeighbors = mostRecentSignalMap.size
    if (equalCount.toFloat / totalNeighbors >= equalityThreshold) {
      changedState = false
      this.state
    } else {
      changedState = true
      ((this.state) + 1) % 2
    }
  }

  override def scoreSignal = if (changedState || lastSignalState == None) 1 else 0

}

/**Builds a Schelling-Segregation simulation on a random grid and executes it**/
object SchellingSegregation extends App {
  val graph = GraphBuilder.build

  //Dimensions of the grid
  val columns = 40
  val rows = 20

  //Create all agents
  for (column <- 0 to columns; row <- 0 to rows) {
    graph.addVertex(new SegregationAgent((column, row), (Math.random * 2.0).floor.toInt, 0.4f))
  }

  // Grid construction: To construct the actual grid we need to connect the neighboring cells.
  for (column <- 0 to columns; row <- 0 to rows) {
    for (neighbor <- neighbors(column, row)) {
      if (inGrid(neighbor._1, neighbor._2)) {
        graph.addEdge(new StateForwarderEdge((column, row), (neighbor._1, neighbor._2)))
      }
    }
  }

  // returns all the neighboring cells of the cell with the given row/column
  def neighbors(column: Int, row: Int): List[(Int, Int)] = {
    List(
      (column - 1, row - 1), (column, row - 1), (column + 1, row - 1),
      (column - 1, row), (column + 1, row),
      (column - 1, row + 1), (column, row + 1), (column + 1, row + 1))
  }

  // tests if a cell is within the grid boundaries
  def inGrid(column: Int, row: Int): Boolean = {
    column >= 0 && row >= 0 && column <= columns && row <= rows
  }

  // creates a string representation of the graph
  def stringRepresentationOfGraph: String = {
    val stringBuilder = new StringBuilder
    for (row <- 0 to rows) {
      for (column <- 0 to columns) {
        stringBuilder.append(graph.forVertexWithId((column, row), (v: SegregationAgent) => v.state).get)
      }
      stringBuilder.append("\n")
    }
    stringBuilder.toString
  }

  println("Grid before:\n" + stringRepresentationOfGraph)

  //Time limit is used to guarantee that the algorithm will terminate
  val stats = graph.execute(ExecutionConfiguration.withTimeLimit(5000))

  //Print general computation statistics
  println(stats)

  println("Grid after:\n" + stringRepresentationOfGraph)

  graph.shutdown
}