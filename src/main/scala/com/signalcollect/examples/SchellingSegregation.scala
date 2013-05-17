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

  def collect = {
    val equalNeighborsCount = (signals filter (_ equals this.state)).size
    val totalNeighborsCount = signals.size
    if (equalNeighborsCount.toFloat / totalNeighborsCount >= equalityThreshold) {
      this.state
    } else {
      (this.state + 1) % 2
    }
  }

}

/**Builds a Schelling-Segregation simulation on a random grid and executes it**/
object SchellingSegregation extends App {
  val graph = GraphBuilder.build

  //Dimensions of the grid
  val columns = 140
  val rows = 40

  println("Adding vertices ...") //Create all cells.
  for (column <- 0 to columns; row <- 0 to rows) {
    graph.addVertex(new SegregationAgent((column, row), (math.random * 2.0).floor.toInt, 0.45f))
  }

  println("Adding edges ...") // Connect the neighboring cells.
  for (column <- 0 to columns; row <- 0 to rows) {
    for (neighbor <- neighbors(column, row)) {
      if (inGrid(neighbor._1, neighbor._2)) {
        graph.addEdge((column, row), new StateForwarderEdge((neighbor._1, neighbor._2)))
      }
    }
  }

  println("Grid before:\n" + stringRepresentationOfGraph)
  val stats = graph.execute
  println(stats) //Print computation statistics
  println("Grid after:\n" + stringRepresentationOfGraph)
  graph.shutdown

  // Returns all the neighboring cells of the cell with the given row/column
  def neighbors(column: Int, row: Int): List[(Int, Int)] = {
    List(
      (column - 1, row - 1), (column, row - 1), (column + 1, row - 1),
      (column - 1, row), (column + 1, row),
      (column - 1, row + 1), (column, row + 1), (column + 1, row + 1))
  }

  // Tests if a cell is within the grid boundaries
  def inGrid(column: Int, row: Int): Boolean = {
    column >= 0 && row >= 0 && column <= columns && row <= rows
  }

  // Creates a string representation of the graph
  def stringRepresentationOfGraph: String = {
    val stateMap = graph.aggregate(new IdStateMapAggregator[(Int, Int), Int])
    val stringBuilder = new StringBuilder
    for (row <- 0 to rows) {
      for (column <- 0 to columns) {
        stringBuilder.append(stateMap((column, row)))
      }
      stringBuilder.append("\n")
    }
    stringBuilder.toString
  }
}