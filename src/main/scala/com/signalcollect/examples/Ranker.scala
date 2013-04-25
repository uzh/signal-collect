package com.signalcollect.examples

import com.signalcollect._
import java.io.FileInputStream
import org.semanticweb.yars.nx.parser.NxParser
import akka.event.Logging
import com.signalcollect.configuration.ExecutionMode

class Publication(id: String, initialState: Double = 0.15)
   extends DataGraphVertex(id, initialState) {
 type Signal = Double
 def collect = {
   initialState + (1 - initialState) * signals.sum
 }
}

class Citation(targetId: String)
   extends DefaultEdge(targetId) {
 def signal = {
   source match {
     case p: Publication => p.state * weight / p.sumOfOutWeights
   }
 }
}

object Ranker extends App {

 val graph = GraphBuilder.withConsole(true, 8091)
                         .withLoggingLevel(Logging.DebugLevel)
                         .build
 val execConfig = ExecutionConfiguration.withExecutionMode(ExecutionMode.Interactive)
 
 println("loading graph")
 
 loadGraph
 
 println("executing")
 
 val stats = graph.execute(execConfig)
 
//  val topPublications = graph.aggregate(new TopKFinder[String, Double](10))
//  
//  topPublications foreach (println(_))
 
 graph.shutdown
 
 def loadGraph {
   val is = new FileInputStream("./references.nt")
   val parser = new NxParser(is)
   var i = 0
   while (parser.hasNext && i < 20000) {
     i += 1
     val triple = parser.next
     val citer = triple(0).toString
     val cited = triple(2).toString
     graph.addVertex(new Publication(citer))
     graph.addVertex(new Publication(cited))
     graph.addEdge(citer, new Citation(cited))
   }

 }

}
