/*
 *  @author Philip Stutz, Mihaela Verman
 *  
 *  Copyright 2012 University of Zurich
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
import com.signalcollect.configuration._
import com.signalcollect.configuration.LoggingLevel._
import scala.util._
import scala.math
import com.signalcollect.interfaces.MessageBus




/**
 * Represents an Agent
 *
 *  @param id: the identifier of this vertex
 *  @param constraints: the set of constraints in which it is involved
 *  @param possibleValues: which values can the state take
 */
class DSANVertex(id: Any, constraints: Iterable[Constraint], possibleValues: Array[Int]) extends DataGraphVertex(id, 0.0) {

  type Signal = Double
  var time: Int = 0
  var oldState = possibleValues(0)
  var utility: Double = 0						
  var numberSatisfied: Int = 0					//number of satisfied constraints
  val constTemp: Double = 100 					//constant for calculating temperature

  /**
   * The collect function chooses a new random state and chooses it if it improves over the old state, 
   * or, if it doesn't it still chooses it (for exploring purposes) with probability decreasing with time 
   */
  def collect(oldState: State, mostRecentSignals: Iterable[Double]): Double = {

    time += 1

    val neighbourConfigs = (mostRecentSignalMap map {
      keyValueTuple =>
        (keyValueTuple._1.sourceId -> keyValueTuple._2.toDouble)
    })
    
    //calculate utility and number of satisfied constraints for the current value
    val configs = neighbourConfigs + (id -> oldState.toDouble)
    utility = (constraints map (_.utility(configs)) sum)
    numberSatisfied = constraints map (_.satisfiesInt(configs)) sum

    // select randomly a value and adopt it with probability (e(delta/t_i)) when delta<=0 (to explore)
    // or with probability 1 otherwise
    val r = new Random()

    //calculate utility and number of satisfied constraints for the new value
    val newStateIndex = r.nextInt(possibleValues.size) 
    val newState = possibleValues(newStateIndex)
    val newconfigs = neighbourConfigs + (id -> newState.toDouble)
    val newStateUtility = (constraints map (_.utility(newconfigs)) sum)
    val newNumberSatisfied = constraints map (_.satisfiesInt(newconfigs)) sum
    
    val delta = newStateUtility - utility


    //choose between new value and old value
    if (delta <= 0) {
      val adopt = r.nextDouble()
      if (adopt < math.exp(delta * time * time/ constTemp)) {
        if (oldState == newState)
        	graphEditor.sendSignalToVertex(0.0, id)
        utility = newStateUtility
        numberSatisfied  = newNumberSatisfied
        println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"<= 0 and changed to state: "+newState +" instead of "+oldState+" with Adoption of new state prob ="+math.exp(delta * time * time/ constTemp)+" ")
        return newState
      }
    } else { //delta>0, we adopt the new state
      utility = newStateUtility
      numberSatisfied = newNumberSatisfied
      println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"> 0 and changed to state: "+newState +" instead of "+oldState)
      return newState
    }
    graphEditor.sendSignalToVertex(0.0, id) //sending myself a dummy value which won't count any way
    println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"<= 0 and NOT changed to state: "+newState +" instead of "+oldState+" with Adoption of new state prob ="+math.exp(delta * time * time/ constTemp)+" ")
    oldState



  }

  override def scoreSignal: Double = {
    lastSignalState match {
      case Some(oldState) => 
        if ((oldState==state)&&(numberSatisfied == constraints.size)){  //computation is allowed to stop if state has not changed and utility is maximized
          0
        } else {
          1
        }
      case other => 1

    }

  }

}

/** Builds an agents graph and executes the computation */
object DSAN extends App {

  val graph = GraphBuilder.withLoggingLevel(LoggingLevel.Debug).build
  
  println("From client: Graph built")
  
// Simple graph with 2 vertices
// 
//  val c12:  Constraint = Variable(1) != Variable(2)
//  
//  graph.addVertex(new DSANVertex(1, Array(c12), Array(0, 1)))
// graph.addVertex(new DSANVertex(2, Array(c12), Array(0, 1)))
// 
// graph.addEdge(new StateForwarderEdge(1, 2))
// graph.addEdge(new StateForwarderEdge(2, 1))
  
  
 //Graph with 6 nodes 

  val c12: Constraint = Variable(1) != Variable(2)
  val c13: Constraint = Variable(1) != Variable(3)
  val c23: Constraint = Variable(3) != Variable(2)
  val c34: Constraint = Variable(3) != Variable(4)
  val c45: Constraint = Variable(5) != Variable(4)
  val c35: Constraint = Variable(5) != Variable(3)
  val c56: Constraint = Variable(5) != Variable(6)
  val c26: Constraint = Variable(6) != Variable(2)

  // Loading Method 2: Non blocking loading

  graph.addVertex(new DSANVertex(1, Array(c12, c13), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(2, Array(c12, c23, c26), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(3, Array(c13, c23, c35, c34), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(4, Array(c34, c45), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(5, Array(c35, c45, c56), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(6, Array(c26, c56), Array(0, 1, 2)))

  graph.addEdge(new StateForwarderEdge(1, 2))
  graph.addEdge(new StateForwarderEdge(1, 3))
  graph.addEdge(new StateForwarderEdge(2, 3))
  graph.addEdge(new StateForwarderEdge(3, 4))
  graph.addEdge(new StateForwarderEdge(4, 5))
  graph.addEdge(new StateForwarderEdge(3, 5))
  graph.addEdge(new StateForwarderEdge(5, 6))
  graph.addEdge(new StateForwarderEdge(2, 6))

  graph.addEdge(new StateForwarderEdge(2, 1))
  graph.addEdge(new StateForwarderEdge(3, 1))
  graph.addEdge(new StateForwarderEdge(3, 2))
  graph.addEdge(new StateForwarderEdge(4, 3))
  graph.addEdge(new StateForwarderEdge(5, 4))
  graph.addEdge(new StateForwarderEdge(5, 3))
  graph.addEdge(new StateForwarderEdge(6, 5))
  graph.addEdge(new StateForwarderEdge(6, 2))

  println("Begin")

  val stats = graph.execute(ExecutionConfiguration().withExecutionMode(ExecutionMode.Synchronous))
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
