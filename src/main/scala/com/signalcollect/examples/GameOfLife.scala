/*
 *  @author Philip Stutz
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
import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.configuration.ExecutionMode

/**
 *  Represents a cell in a "Conway's Game of Life" (http://en.wikipedia.org/wiki/Conway's_Game_of_Life) simulation
 *
 *  @param id: the identifier of this vertex
 *  @param initialState: initial state of the agent: 0 = dead, 1 = alive
 */
class GameOfLifeCell(id: Any, initialState: Int) extends DataGraphVertex(id, initialState) {
  type Signal = Int

  def collect(oldState: Int, mostRecentSignals: Iterable[Int], graphEditor: GraphEditor): Int = {
    val numberOfAliveNeighbors = mostRecentSignals.sum
    numberOfAliveNeighbors match {
      case 0 => 0 // dies of loneliness
      case 1 => 0 // dies of loneliness
      case 2 => oldState // same as before
      case 3 => 1 // becomes alive if dead
      case higher => 0 // dies of overcrowding
    }
  }

}

/**
 * Builds a "Conway's Game of Life" (http://en.wikipedia.org/wiki/Conway's_Game_of_Life)
 * simulation on a random grid and executes it
 */
object GameOfLife extends App {
  val graph = GraphBuilder.build

  //Dimensions of the grid
  val columns = 100
  val rows = 100
  val generations = 10000

  println("Adding vertices ...") //Create all cells.
  for (column <- 0 to columns; row <- 0 to rows) {
    graph.addVertex(new GameOfLifeCell((column, row), (math.random * 2.0).floor.toInt))
  }

  println("Adding edges ...") // Connect the neighboring cells.
  for (column <- 0 to columns; row <- 0 to rows) {
    for (neighbor <- neighbors(column, row)) {
      if (inGrid(neighbor._1, neighbor._2)) {
        graph.addEdge((column, row), new StateForwarderEdge((neighbor._1, neighbor._2)))
      }
    }
  }

  val execConfig = ExecutionConfiguration.withExecutionMode(ExecutionMode.Synchronous).withStepsLimit(1)

  for (i <- 0 to generations) {
//    println(stringRepresentationOfGraph)
    val stats = graph.execute(execConfig)
  }
//  println(stringRepresentationOfGraph)

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
        val state = stateMap((column, row))
        val symbol = state match {
          case 0 => " "
          case other => "x"
        }
        stringBuilder.append(symbol)
      }
      stringBuilder.append("\n")
    }
    stringBuilder.toString
  }
}