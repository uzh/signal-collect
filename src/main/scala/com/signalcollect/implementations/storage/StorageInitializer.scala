//package com.signalcollect.implementations.storage
//
//import com.signalcollect.interfaces.{Storage, Vertex, Edge}
//import com.signalcollect.implementations.coordinator.WorkerApi
//import java.io._
//
//trait StorageInitializer extends WorkerApi {
////	val storages = new Array[Storage](workerApi.config.numberOfWorkers)
////	//initialize storages array where index equals worker id
////    for (workerId <- 0 until workerApi.config.numberOfWorkers) {
////	  storages(workerId) = workerApi.workers(workerId).getStore
////	}
//	
//	def initializeVerticesFromFile(filePath : String, parser: (String) => Vertex[_,_]) {
//	  val reader = new BufferedReader(new FileReader(filePath))
//	  var line = reader.readLine
//	  while(line!=null){
//	    if(line.trim.length>0) {
//	      val vertex = parser(line)
//	      workers(vertex.id.hashCode).addVertex(vertex)
//	    }
//	  }
//	}
//	
//	def initializeEdgesFromFile(filePath : String, parser: (String) => Edge[_,_]) {
//	  val reader = new BufferedReader(new FileReader(filePath))
//	  var line = reader.readLine
//	  while(line!=null){
//	    if(line.trim.length>0) {
//	      val edge = parser(line)
//	      workers(edge.sourceId.hashCode).addEdge(edge)
//	    }
//	  }
//	}
//}