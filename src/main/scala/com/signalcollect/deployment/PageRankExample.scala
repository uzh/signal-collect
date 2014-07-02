/**
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

package com.signalcollect.deployment

import com.signalcollect.GraphBuilder
import com.signalcollect.examples.PageRankVertex
import com.signalcollect.examples.PageRankEdge
import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.signalcollect.Graph

object PageRankExample extends DeployableAlgorithm with App {
  override def loadGraph(graph: Graph[Any,Any]): Graph[Any,Any] = {
     println("add vertices")
    graph.addVertex(new PageRankVertex(1))
    graph.addVertex(new PageRankVertex(2))
    graph.addVertex(new PageRankVertex(3))
    println("add edges")
    graph.addEdge(1, new PageRankEdge(2))
    graph.addEdge(2, new PageRankEdge(1))
    graph.addEdge(2, new PageRankEdge(3))
    graph.addEdge(3, new PageRankEdge(2))
    graph
  }
//  override def execute(parameters: Map[String, String], graphBuilder: GraphBuilder[Any, Any]) {
//
//    val graph =  graphBuilder.build
//    println("add vertices")
//    graph.addVertex(new PageRankVertex(1))
//    graph.addVertex(new PageRankVertex(2))
//    graph.addVertex(new PageRankVertex(3))
//    println("add edges")
//    graph.addEdge(1, new PageRankEdge(2))
//    graph.addEdge(2, new PageRankEdge(1))
//    graph.addEdge(2, new PageRankEdge(3))
//    graph.addEdge(3, new PageRankEdge(2))
//    println("Graph has been built, awaiting idle ...")
//    graph.awaitIdle
//    println("Executing computation ...")
//    val stats = graph.execute
//    println(stats)
//    graph.shutdown
//  }
}

//object ReflectTest extends App{
//
//  val clazz = Class.forName("com.signalcollect.deployment.MyObject$")
//  val myObj = clazz.getField("MODULE$").get(classOf[MyTrait]).asInstanceOf[MyTrait]
//  println(myObj.method1(2))
//}
//
//trait MyTrait {
//  def method1(a: Int):Int
//}
//
//object MyObject extends MyTrait {
//  def method1(a: Int) = a + 1
//}
