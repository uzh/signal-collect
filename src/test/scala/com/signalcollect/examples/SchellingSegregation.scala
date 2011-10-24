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
  val m = 40
  val n = 20

  //Create all agents
  for (i <- 0 until (m * n)) {
    graph.addVertex(new SegregationAgent(i, (Math.random * 2.0).floor.toInt, 0.4f))
  }

  /* 
   * Grid construction
   * 
   * To construct the actual grid we need to connect the neighboring cells.
   * The following sketch shows all the neighboring 
   * cells that a cell "XX" needs to be connected to:
   * 
   * N1 | N2 | N3
   * ------------
   * N4 | XX | N5
   * ------------
   * N6 | N7 | N8
   * 
   * The names N1..N8 can also be found in the code to show the connection
   * that is currently drawn.
   * 
   * We further need to make sure that cells only link to cells within the grid
   */
  for (i <- 0 until m) {
    for (j <- 0 until n) {
      if ((i - 1) >= 0) { //Make sure all neighbors left of the cell are within the grid
        graph.addEdge(new StateForwarderEdge(j * m + i, j * m + i - 1)) //N4
        if ((j - 1) >= 0) {
          graph.addEdge(new StateForwarderEdge(j * m + i, (j - 1) * m + i - 1)) //N1
        }
        if ((j + 1) < n) {
          graph.addEdge(new StateForwarderEdge(j * m + i, (j + 1) * m + i - 1)) //N6
        }
      }
      if ((i + 1) < m) { //Make sure all neighbors right of the cell are within the grid
        graph.addEdge(new StateForwarderEdge(j * m + i, j * m + i + 1)) //N5
        if ((j - 1) >= 0) {
          graph.addEdge(new StateForwarderEdge(j * m + i, (j - 1) * m + i + 1)) //N3
        }
        if ((j + 1) < n) {
          graph.addEdge(new StateForwarderEdge(j * m + i, (j + 1) * m + i + 1)) //N8
        }
      }
      if ((j - 1) >= 0) { // Top neighbor
        graph.addEdge(new StateForwarderEdge(j * m + i, (j - 1) * m + i)) //N2
      }
      if ((j + 1) < n) { // Bottom neighbor
        graph.addEdge(new StateForwarderEdge(j * m + i, (j + 1) * m + i)) //N7
      }
    }
  }

  //Print the start grid
  println("Grid before:")
  for (i <- 0 until (m * n)) {
    if (i % m == 0) {
      print("\n")
    }
    val status: Option[Int] = graph.forVertexWithId(i, (v: SegregationAgent) => v.state)
    print(status.get)
  }

  //Time limit is used to guarantee that the algorithm will terminate
  val stats = graph.execute(ExecutionConfiguration.withTimeLimit(5000))

  //Print general computation statistics
  println(stats)

  //Print the resulting grid
  println("Grid after:")
  for (i <- 0 until (m * n)) {
    if (i % m == 0) {
      print("\n")
    }
    val status: Option[Int] = graph.forVertexWithId(i, (v: SegregationAgent) => v.state)
    print(status.get)
  }

  graph.shutdown
}