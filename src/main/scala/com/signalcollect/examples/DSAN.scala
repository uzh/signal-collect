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

/*
 * Implementation of Distributed Simulated Annealing 
 * ( Arshad, Silaghi, 2003. "Distributed Simulated Annealing and comparison to DSA". 
 *  In Proceedings of the 4th International Workshop on Distributed Contraint Reasoning, Acapulco, Mexico)
 */

package com.signalcollect.examples

import com.signalcollect._
import com.signalcollect.configuration._
import com.signalcollect.configuration.LoggingLevel._
import scala.util._
import scala.math
import com.signalcollect.interfaces.MessageBus
import com.signalcollect.interfaces.EdgeId




/**
 * Represents an Agent
 *
 *  @param id: the identifier of this vertex
 *  @param constraints: the set of constraints in which it is involved
 *  @param possibleValues: which values can the state take
 */
class DSANVertex(id: Any, constraints: Iterable[Constraint], possibleValues: Array[Int]) extends DataGraphVertex(id, 0.0) {

  type Signal = Double
  var time: Int = 0								//time counter used for calculating temperature
  var oldState = possibleValues(0)
  var utility: Double = 0						
  var numberSatisfied: Int = 0					//number of satisfied constraints
  val constTemp: Double = 100 					//constant for calculating temperature

  /**
   * The collect function chooses a new random state and chooses it if it improves over the old state, 
   * or, if it doesn't it still chooses it (for exploring purposes) with probability decreasing with time 
   */
  def collect(oldState: Double, mostRecentSignals: Iterable[Double], graphEditor: GraphEditor): Double = {
    
    time += 1

    val neighbourConfigs = (mostRecentSignalMap map {
      keyValueTuple =>
        (keyValueTuple._1 -> keyValueTuple._2.toDouble)
    })
    
    																			//Calculate utility and number of satisfied constraints for the current value
    val configs = neighbourConfigs + (id -> oldState)
    utility = (constraints map (_.utility(configs)) sum)
    numberSatisfied = constraints map (_.satisfiesInt(configs)) sum

																				// Select randomly a value and adopt it with probability (e(delta/t_i)) when delta<=0 (to explore)
																				// or with probability 1 otherwise
    val r = new Random()

																				//Calculate utility and number of satisfied constraints for the new value
    val newStateIndex = r.nextInt(possibleValues.size) 
    val newState = possibleValues(newStateIndex)
    val newconfigs = neighbourConfigs + (id -> newState.toDouble)
    val newStateUtility = (constraints map (_.utility(newconfigs)) sum)
    val newNumberSatisfied = constraints map (_.satisfiesInt(newconfigs)) sum
    
    																			// delta is the difference between the utility of the new randomly selected state and the utility of the old state. 
    																			// It is > 0 if the new state would lead to improvements
    val delta = newStateUtility - utility 

  /**
   * The actual algorithm:
   * 	If new state does not improve the utility (delta<=0), select it for exploring purposes with probability (e(delta/t_i)) or else keep the old state
   * 	If new state does improve the utility (delta>0), select it instead of the old state
   * t_i is the temperature which has to be a decreasing function of time. For this particular case we chose t_i = constTemp/(time*time) 
   */

    
    if (delta <= 0) { 												//The new state does not improve utility
      val adopt = r.nextDouble()
      if (adopt < math.exp(delta * time * time/ constTemp)) {  			// We choose the new state (to explore) over the old state with probability (e(delta/t_i))
        if (oldState == newState) 											//We send a dummy value to self to avoid blocking
        	graphEditor.sendSignal(0.0, EdgeId(null, id))
        utility = newStateUtility
        numberSatisfied = newNumberSatisfied
        println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"<= 0 and changed to state: "+newState +" instead of "+oldState+" with Adoption of new state prob ="+math.exp(delta * time * time/ constTemp)+" ")
        return newState
      } else { 															//With probability 1 - (e(delta/t_i)) we keep the old state which is better
        graphEditor.sendSignal(0.0, EdgeId(null, id)) 							//We send a dummy value to self to avoid blocking
        println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"<= 0 and NOT changed to state: "+newState +" instead of "+oldState+" with Adoption of new state prob ="+math.exp(delta * time * time/ constTemp)+" ")
        return oldState
      }
    } else { 														//The new state improves utility (delta>0), so we adopt the new state
      utility = newStateUtility
      numberSatisfied = newNumberSatisfied
      println("Vertex: "+id+" at time "+time+"; Case DELTA="+delta+"> 0 and changed to state: "+newState +" instead of "+oldState)
      return newState
    }
    



  }//end collect function

  override def scoreSignal: Double = {
    lastSignalState match {
      case Some(oldState) => 
        if ( ( oldState == state ) && ( numberSatisfied == constraints.size ) ){  //computation is allowed to stop only if state has not changed and the utility is maximized
          0
        } else {
          1
        }
      case other => 1

    }

  }//end scoreSignal

}//end DSANVertex class

/** Builds an agents graph and executes the computation */
object DSAN extends App {

  val graph = GraphBuilder.withLoggingLevel(LoggingLevel.Debug).build
  
  println("From client: Graph built")
  
  //Simple graph with 2 vertices
 
//  val c12:  Constraint = Variable(1) != Variable(2)
//  
//  graph.addVertex(new DSANVertex(1, Array(c12), Array(0, 1)))
// 	graph.addVertex(new DSANVertex(2, Array(c12), Array(0, 1)))
// 
// 	graph.addEdge(new StateForwarderEdge(1, 2))
// 	graph.addEdge(new StateForwarderEdge(2, 1))
  
  
  //Graph with 6 nodes 

  val c12: Constraint = Variable(1) != Variable(2)
  val c13: Constraint = Variable(1) != Variable(3)
  val c23: Constraint = Variable(3) != Variable(2)
  val c34: Constraint = Variable(3) != Variable(4)
  val c45: Constraint = Variable(5) != Variable(4)
  val c35: Constraint = Variable(5) != Variable(3)
  val c56: Constraint = Variable(5) != Variable(6)
  val c26: Constraint = Variable(6) != Variable(2)



  graph.addVertex(new DSANVertex(1, Array(c12, c13), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(2, Array(c12, c23, c26), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(3, Array(c13, c23, c35, c34), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(4, Array(c34, c45), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(5, Array(c35, c45, c56), Array(0, 1, 2)))
  graph.addVertex(new DSANVertex(6, Array(c26, c56), Array(0, 1, 2)))

  graph.addEdge(1, new StateForwarderEdge(2))
  graph.addEdge(1, new StateForwarderEdge(3))
  graph.addEdge(2, new StateForwarderEdge(3))
  graph.addEdge(3, new StateForwarderEdge(4))
  graph.addEdge(4, new StateForwarderEdge(5))
  graph.addEdge(3, new StateForwarderEdge(5))
  graph.addEdge(5, new StateForwarderEdge(6))
  graph.addEdge(2, new StateForwarderEdge(6))

  graph.addEdge(2, new StateForwarderEdge(1))
  graph.addEdge(3, new StateForwarderEdge(1))
  graph.addEdge(3, new StateForwarderEdge(2))
  graph.addEdge(4, new StateForwarderEdge(3))
  graph.addEdge(5, new StateForwarderEdge(4))
  graph.addEdge(5, new StateForwarderEdge(3))
  graph.addEdge(6, new StateForwarderEdge(5))
  graph.addEdge(6, new StateForwarderEdge(2))

  println("Begin")

  val stats = graph.execute(ExecutionConfiguration().withExecutionMode(ExecutionMode.Synchronous))
  println(stats)
  graph.foreachVertex(println(_))
  graph.shutdown
}
