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
import collection.mutable.{ ListMap, HashMap, SynchronizedMap }

/**
 * Represents all associated Sudoku cells that have to be taken into account to determine
 * the value of a cell
 *
 */
class SudokuAssociation(t: Any) extends OptionalSignalEdge(t) {
  def signal(sourceVertex: Vertex[_, _]) = sourceVertex.asInstanceOf[SudokuCell].state
}

/**
 * Cell in a Sudoku grid.
 *
 * @param id ID of the cell, where top left cell has id=0 top right has id of 8 and bottom right has id 80
 *
 */
class SudokuCell(id: Int, initialState: Option[Int] = None) extends DataGraphVertex(id, initialState) {

  type Signal = Int

  var possibleValues = SudokuHelper.legalNumbers
  if (initialState.isDefined) possibleValues = Set(initialState.get)

  def collect(oldState: Option[Int], mostRecentSignals: Iterable[Int], graphEditor: GraphEditor): Option[Int] = {

    //make a list of all possible values
    possibleValues = possibleValues -- mostRecentSignals.toSet

    //If own value is determined i.e. if only one possible value is left choose own value
    if (possibleValues.size == 1) {
      Some(possibleValues.head)
    } else
      oldState
  }
}

object Sudoku extends App {
  //Setting the values that are given, rest has default value 'None'

  //Very simple Sudoku
  val sudoku1 = Map(
    4 -> 9,
    5 -> 6,
    8 -> 5,
    10 -> 9,
    11 -> 4,
    13 -> 2,
    14 -> 1,
    15 -> 8,
    16 -> 6,
    19 -> 1,
    21 -> 4,
    24 -> 3,
    25 -> 2,
    29 -> 3,
    31 -> 4,
    34 -> 7,
    36 -> 1,
    38 -> 6,
    42 -> 4,
    44 -> 2,
    46 -> 4,
    49 -> 6,
    51 -> 5,
    55 -> 5,
    56 -> 2,
    59 -> 4,
    61 -> 1,
    64 -> 6,
    65 -> 1,
    66 -> 2,
    67 -> 3,
    69 -> 7,
    70 -> 8,
    72 -> 4,
    75 -> 8,
    76 -> 1)

  //bad-ass Sudoku Puzzle
  val sudoku2 = Map(
    0 -> 9, 8 -> 4,
    11 -> 5, 13 -> 3, 15 -> 8, 16 -> 9,
    21 -> 6, 24 -> 2,
    28 -> 9, 31 -> 8, 33 -> 3, 35 -> 7,
    38 -> 1, 42 -> 4,
    45 -> 7, 47 -> 3, 49 -> 2, 52 -> 8,
    56 -> 9, 59 -> 6,
    64 -> 7, 65 -> 8, 67 -> 5, 69 -> 1,
    72 -> 6, 80 -> 3)

  //select a sudoku puzzle
  val initialSeed = sudoku1

  var graph = computeGraphFactory(initialSeed)

  //print initial Sudoku
  var seed = new HashMap[Int, Option[Int]]()
  graph.foreachVertex { v => seed += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
  SudokuHelper.printSudoku(seed)

  val stats = graph.execute
  println(stats)

  //If simple constraint propagation did not solve the problem apply a depth first search algorithm to find a suitable solution
  if (!isDone(graph)) {
    graph = tryPossibilities(graph)
    if (graph == null) {
      println()
      println("Sorry this Sudoku is not solvable")
      sys.exit(5)
    }
  }
  println()

  var result = new HashMap[Int, Option[Int]]() with SynchronizedMap[Int, Option[Int]]
  graph.foreachVertex { v => result += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
  graph.shutdown
  SudokuHelper.printSudoku(result)

  /**
   * Check if all cells have a value assigned to it
   */
  def isDone(graph: Graph): Boolean = {
    var isDone = true
    graph.foreachVertex(v => if (v.state.asInstanceOf[Option[Int]] == None) isDone = false)
    isDone
  }

  /**
   * Recursive depth first search for possible values
   */
  def tryPossibilities(graph: Graph): Graph = {

    val possibleValues = new ListMap[Int, Set[Int]]()
    graph.foreachVertex(v => possibleValues.put(v.id.asInstanceOf[Int], v.asInstanceOf[SudokuCell].possibleValues))
    graph.shutdown

    var solutionFound = false

    // Try different values for the cell with the highest probability for each possible value i.e. the one with
    // the smallest number of alternatives.
    def mostConstrainedCell(possibilites: ListMap[Int, Set[Int]]): Option[(Int, Set[Int])] = {
      val mostConstrained = possibilites.toList.filter(_._2.size > 1).sortBy(_._2.size) // Just selects the smallest set of possible values among the cells
      if (mostConstrained.size == 0) {
        None //dead end
      } else {
        Some(mostConstrained.head)
      }
    }

    val candidate = mostConstrainedCell(possibleValues)

    if (candidate != None) {
      val iterator = candidate.get._2.iterator
      while (iterator.hasNext && !solutionFound) {
        var determinedValues = possibleValues.filter(_._2.size == 1).map(x => (x._1, x._2.head)).toMap[Int, Int]
        determinedValues += (candidate.get._1 -> iterator.next)
        var graphTry = computeGraphFactory(determinedValues)
        graphTry.execute
        if (isDone(graphTry)) {
          solutionFound = true
          return graphTry
        } else {
          val nextTryResult = tryPossibilities(graphTry)
          if (nextTryResult != null) {
            return nextTryResult
          }
        }
      }
    }
    null
  }

  def computeGraphFactory(seed: Map[Int, Int]): Graph = {
    val graph = GraphBuilder.build

    //Add all Cells for Sudoku
    for (index <- 0 to 80) {
      val seedValue = seed.get(index)
      graph.addVertex(new SudokuCell(index, seedValue))
    }

    //Determine neighboring cells for each cell and draw the edges between them
    for (index <- 0 to 80) {
      SudokuHelper.cellsToConsider(index).foreach({ i =>
        graph.addEdge(i, new SudokuAssociation(index))
      })
    }
    graph
  }

}

/**
 * Provides useful utilites for dealing with sudoku grids
 *
 */
object SudokuHelper {
  //All possible numbers for a cell
  val legalNumbers = {
    var numbers = (1 to 9).toSet
    println
    numbers
  }

  //Get Rows, Columns and Blocks from ID
  def getRow(id: Int) = id / 9
  def getColumn(id: Int) = id % 9
  def getBlock(id: Int) = getRow(id) * 3 + getColumn(id)

  /**
   * Returns all the neighboring cells that influence a cell's value
   */
  def cellsToConsider(id: Int): List[Int] = {
    var neighborhood = List[Int]()

    //Same row
    for (col <- 0 to 8) {
      val otherID = getRow(id) * 9 + col
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same column
    for (row <- 0 to 8) {
      val otherID = row * 9 + getColumn(id)
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same block
    val topLeftRow = (getRow(id) / 3) * 3
    val topLeftColumn = (getColumn(id) / 3) * 3

    for (row <- topLeftRow to (topLeftRow + 2)) {
      for (column <- topLeftColumn to (topLeftColumn + 2)) {
        val otherID = row * 9 + column

        if (otherID != id && !neighborhood.contains(otherID)) {
          neighborhood = otherID :: neighborhood
        }
      }
    }

    neighborhood
  }

  /**
   * Formats the data in a classical sudoku layout
   */
  def printSudoku(data: HashMap[Int, Option[Int]]) = {

    println()
    println("Sudoku")
    println("======")
    println()
    println("=========================================")

    for (i <- 0 to 8) {
      val j = i * 9
      print("II")
      for (k <- j to j + 8) {
        data.get(k) match {
          case Some(Some(v)) => print(" " + v + " ")
          case v => print("   ") //Empty or Error
        }
        if (k % 3 == 2) {
          print("II")
        } else {
          print("|")
        }
      }
      println()
      if (i % 3 == 2) {
        println("=========================================")
      }
    }
  }
}