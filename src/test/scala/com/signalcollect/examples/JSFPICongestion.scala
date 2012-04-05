//package com.signalcollect.examples
//
//import com.signalcollect._
//import com.signalcollect.configuration._
//
//
//class Player(id: Any, noActions: Int) extends DataGraphVertex(id, noActions){
//  
//  type Signal = Int
//  
//  val stateValue = new Array[Int](noActions) 
//  val congestions = new Array[Int](noActions)
//  
//  def utility(congestion: Int): Double = {
//    -congestion-1;
//  }
//  
//  def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
//    
//  }
//  
//  
//  
//}
//
//class Resource(id: Any, congestion: Int) extends DataGraphVertex(id, congestion){
//	type Signal = Int
//	
//	def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
//	  congestion + mostRecentSignals.sum
//	}
//}
//
//
//class PageRankVertex(id: Any, dampingFactor: Double = 0.85) extends DataGraphVertex(id, 1 - dampingFactor) {
//
//  type Signal = Double
//
//  /**
//   * The collect function calculates the rank of this vertex based on the rank
//   *  received from neighbors and the damping factor.
//   */
//  def collect(oldState: State, mostRecentSignals: Iterable[Double]): Double = {
//    1 - dampingFactor + dampingFactor * mostRecentSignals.sum
//  }
//}
//
//object JSFPI extends App {
//  
// 
//  
//  
//
//}