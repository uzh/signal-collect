/*
 *  @author Philip Stutz
 *  @author Carol Alexandru
 *  
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.console

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net._
import scala.language.postfixOps
import scala.Array.canBuildFrom
import scala.collection.immutable.List.apply
import com.signalcollect.interfaces.{ Coordinator, WorkerStatistics, 
                                      SystemInformation, Inspectable }
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.TopKFinder
import com.sun.net.httpserver.{ HttpExchange, HttpHandler, HttpServer }
import akka.actor.ActorRef
import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.Vertex

import scala.util.Random

import org.java_websocket._
import org.java_websocket.WebSocketImpl
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import scala.collection.JavaConversions._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

class ConsoleServer(userHttpPort: Int) {

  val (server: HttpServer, sockets: WebSocketConsoleServer) = setupUserPorts(userHttpPort)
  
  server.createContext("/", new FileServer("web-data"))
  server.createContext("/api", new ApiServer())
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("HTTP server started on http://localhost:" + server.getAddress().getPort() + "")

  sockets.start();
  println("WebSocket - Server started on port: " + sockets.getPort())

  def setupUserPorts(httpPort: Int) : (HttpServer, WebSocketConsoleServer) = {
    val minAllowedUserPortNumber = 1025
    if (httpPort < minAllowedUserPortNumber) {
      val defaultPort = 8080
      val maxUserPort = 8179
      println("Websocket - No valid port given (using default port " + defaultPort + ")")
      for (port <- defaultPort to maxUserPort) {
        try {
          println("Websocket - Connecting to port " + port + "...")
          return getNewServers(port)
        } catch {
          case e: Exception => println("Websocket - Starting server on port " + port + " failed: " + e.getMessage())
        }
      }
      println("Could not start server on ports " + defaultPort + " to " + maxUserPort)
      sys.exit
    } else {
      try {
        return getNewServers(httpPort)
      } catch {
        case e: Throwable => println("Could not start server: " + e.getMessage()); sys.exit
      }
    }
  }
  
  def getNewServers(httpPort: Int) = {
    val server: HttpServer = HttpServer.create(new InetSocketAddress(httpPort), 0)
    val sockets: WebSocketConsoleServer = new WebSocketConsoleServer(new InetSocketAddress(httpPort + 100));
    (server, sockets)
  }
  
  def setCoordinator(coordinatorActor: ActorRef) {
    sockets.setCoordinator(coordinatorActor)
  }

  def shutdown {
    server.stop(0)
    sockets.stop(0)
  }
}

class ApiServer() extends HttpHandler {
  def handle(t: HttpExchange) {
    var target = t.getRequestURI.getPath
    println(target)
  }
}
class FileServer(folderName: String) extends HttpHandler {
  def handle(t: HttpExchange) {

    def root = "./" + folderName
    var target = t.getRequestURI.getPath.replaceFirst("^[/.]*", "") 
    if (target == "" || target == "graph" || target == "resources") { target = "main.html" }
    val fileType = target match {
      case t if t.matches(".*\\.html$") => "text/html"
      case t if t.matches(".*\\.css$")  => "text/css"
      case t if t.matches(".*\\.js$")   => "application/javascript"
      case t if t.matches(".*\\.png$")  => "image/png"
      case t if t.matches(".*\\.svg$")  => "image/svg+xml"
      case t if t.matches(".*\\.ico$")  => "image/x-icon"
      case otherwise                    => "text/plain"
    }

    def os = t.getResponseBody
    try {
      val file = new BufferedInputStream(
                 new FileInputStream(root + "/" + target))
      t.getResponseHeaders.set("Content-Type", fileType)
      t.sendResponseHeaders(200, 0)
      Iterator 
        .continually (file.read)
        .takeWhile (-1 !=)
        .foreach (os.write)
      file.close
    }
    catch {
      case e: Exception => t.sendResponseHeaders(400, 0)
    }
    finally {
      os.close
    }
  }
}

class WebSocketConsoleServer(port: InetSocketAddress)
                             extends WebSocketServer(port) {
  var coordinator: Option[Coordinator[_,_]] = None;

  def setCoordinator(coordinatorActor: ActorRef) {
    println("ConsoleServer: got coordinator ActorRef")
    coordinator = Some(AkkaProxy.newInstance[Coordinator[_, _]]
                      (coordinatorActor))
  }

  def onError(socket: WebSocket, ex: Exception) {
    println("WebSocket - an error occured: " + ex)
    ex.printStackTrace()
  }

  def onMessage(socket: WebSocket, msg: String) {
    val j = parse(msg)
    implicit val formats = DefaultFormats
    val p = (j \ "provider").extract[String]
    def provider: DataProvider = coordinator match {
      case Some(c) => p match {
        case "graph" => new GraphDataProvider(c, j)
        case "resources" => new ResourcesDataProvider(c, j)
        case otherwise => new InvalidDataProvider(msg)
      }
      case None => new NotReadyDataProvider(msg)
    }
    socket.send(provider.fetch)
  }

  def onOpen(socket: WebSocket, handshake:ClientHandshake) {
    println("WebSocket - client connected: " + 
            socket.getRemoteSocketAddress.getAddress.getHostAddress)
    }

  def onClose(socket: WebSocket, code: Int, reason: String, remote: Boolean) {
    println("WebSocket - client disconected: " + 
            socket.getRemoteSocketAddress.getAddress.getHostAddress)
  }

}

trait DataProvider {
  def fetch(): String
}

class InvalidDataProvider(msg: String) extends DataProvider {
  def fetch(): String = {
    return compact(render((
      ("provider" -> "invalid") ~
      ("msg" -> ("Received an invalid message: " + msg))
    )))
  }
}

class NotReadyDataProvider(msg: String) extends DataProvider {
  def fetch(): String = {
    return compact(render((
      ("provider" -> "notready") ~
      ("msg" -> "The signal/collect computation is not ready yet") ~
      ("request" -> msg)
    )))
  }
}

case class GraphDataRequest(
  provider: String, 
  search: Option[String], 
  id: Option[String],
  property: Option[Int]
)

/*
  Nodes: Map[String,String]
  Edges: Map[String,List[String]
*/
class GraphDataProvider(coordinator: Coordinator[_, _], msg: JValue) 
      extends DataProvider {
  implicit val formats = DefaultFormats
  val workerApi = coordinator.getWorkerApi 
  def fetch(): String = {
    val request = (msg).extract[GraphDataRequest]
    println(request)
    val graphData = request.search match {
      case Some("vicinity") => request.id match {
        case Some(id) =>
          var vertexAggregator = new SearchAggregator(List(id))
          var nodesEdges = workerApi.aggregateAll(vertexAggregator)
          vertexAggregator = new SearchAggregator(nodesEdges._2(id))
          val nodesEdges2 = workerApi.aggregateAll(vertexAggregator)
          ("provider" -> "graph") ~
          ("nodes" -> nodesEdges2._1) ~
          ("edges" -> nodesEdges2._2)
        case otherwise => 
          ("provider" -> "graph") ~
          ("nodes" -> "") ~
          ("edges" -> "")
      }
      case Some("topk") => request.property match {
        case Some(property) => 
          println(property)
          val topk = new TopKFinder[Int](property)
          val nodes = workerApi.aggregateAll(topk)
          var vertexAggregator = new SearchAggregator(nodes.toList.map(_._1.toString))
          var nodesEdges = workerApi.aggregateAll(vertexAggregator)
          ("provider" -> "graph") ~
          ("nodes" -> nodesEdges._1) ~
          ("edges" -> nodesEdges._2)
        case otherwise => 
          ("provider" -> "graph") ~
          ("nodes" -> "") ~
          ("edges" -> "")
      }
      case otherwise => {
        val vertexAggregator = new AllVerticesAggregator
        val edgeAggregator = new AllEdgesAggregator
        ("provider" -> "graph") ~
        ("nodes" -> workerApi.aggregateAll(vertexAggregator)) ~
        ("edges" -> workerApi.aggregateAll(edgeAggregator)) 
      }

    }
    //println(compact(render(graphData)))
    return compact(render(graphData))
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _], msg: JValue)
      extends DataProvider {
  implicit val formats = DefaultFormats
  def fetch(): String = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: List[WorkerStatistics] = 
      (coordinator.getWorkerApi.getIndividualWorkerStatistics)

    var si: List[SystemInformation] = 
      (coordinator.getWorkerApi.getIndividualSystemInformation)

    val resourceData = (
      ("provider" -> "resources") ~
      ("timestamp" -> System.currentTimeMillis) ~
      ("inboxSize" -> inboxSize) ~
      ("workerStatistics" ->
        /* from WorkerStatistics */
        ("workerId" -> ws.map(_.workerId)) ~
        ("messagesSent" -> ws.map(_.messagesSent.toList)) ~
        ("messagesReceived" -> ws.map(_.messagesReceived)) ~
        ("toSignalSize" -> ws.map(_.toSignalSize)) ~
        ("toCollectSize" -> ws.map(_.toCollectSize)) ~
        ("collectOperationsExecuted" -> ws.map(_.collectOperationsExecuted)) ~
        ("signalOperationsExecuted" -> ws.map(_.signalOperationsExecuted)) ~
        ("numberOfVertices" -> ws.map(_.numberOfVertices)) ~
        ("verticesAdded" -> ws.map(_.verticesAdded)) ~
        ("verticesRemoved" -> ws.map(_.verticesRemoved)) ~
        ("numberOfOutgoingEdges" -> ws.map(_.numberOfOutgoingEdges)) ~
        ("outgoingEdgesAdded" -> ws.map(_.outgoingEdgesAdded)) ~
        ("outgoingEdgesRemoved" -> ws.map(_.outgoingEdgesRemoved)) ~
        ("receiveTimeoutMessagesReceived" -> ws.map(_.receiveTimeoutMessagesReceived)) ~
        ("heartbeatMessagesReceived" -> ws.map(_.heartbeatMessagesReceived)) ~
        ("signalMessagesReceived" -> ws.map(_.signalMessagesReceived)) ~
        ("bulkSignalMessagesReceived" -> ws.map(_.bulkSignalMessagesReceived)) ~
        ("continueMessagesReceived" -> ws.map(_.continueMessagesReceived)) ~
        ("requestMessagesReceived" -> ws.map(_.requestMessagesReceived)) ~
        ("otherMessagesReceived" -> ws.map(_.otherMessagesReceived)) ~
        /* from SystemInformation */
        ("os" -> si.map(_.os))) ~
        ("runtime_mem_total" -> si.map(_.runtime_mem_total)) ~
        ("runtime_mem_max" -> si.map(_.runtime_mem_max)) ~
        ("runtime_mem_free" -> si.map(_.runtime_mem_free)) ~
        ("runtime_cores" -> si.map(_.runtime_cores)) ~
        ("jmx_committed_vms" -> si.map(_.jmx_committed_vms)) ~
        ("jmx_mem_free" -> si.map(_.jmx_mem_free)) ~
        ("jmx_mem_total" -> si.map(_.jmx_mem_total)) ~
        ("jmx_swap_free" -> si.map(_.jmx_swap_free)) ~
        ("jmx_swap_total" ->si.map(_.jmx_swap_total)) ~
        ("jmx_process_load" -> si.map(_.jmx_process_load)) ~
        ("jmx_process_time" -> si.map(_.jmx_process_time)) ~
        ("jmx_system_load" -> si.map({ s =>
          val v = s.jmx_system_load
          if (v.isNaN()) { 0 } 
          else {v}})) 
    )
    return compact(render(resourceData))

    /* code kept for reference; use something like this to genericise?
    val srcVal: Any = w.getClass.getMethods.find(_.getName == h).get.invoke(w)
    */

  }
}

class AllVerticesAggregator extends AggregationOperation[Map[String, String]] {
  def extract(v: Vertex[_, _]): Map[String,String] = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => Map()
  }

  def reduce(vertices: Stream[Map[String,String]]): Map[String,String] = {
    vertices.foldLeft(Map[String,String]())((acc:Map[String,String], 
                                               v:Map[String,String]) => acc ++ v)
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): Map[String,String] = {
    Map(v.id.toString -> v.state.toString)
  }
}

class AllEdgesAggregator
      extends AggregationOperation[Map[String,List[String]]] {

  type EdgeMap = Map[String,List[String]]

  def extract(v: Vertex[_, _]): EdgeMap = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
  }

  def reduce(vertices: Stream[EdgeMap]):
                              EdgeMap = {
    vertices.foldLeft(Map[String,List[String]]())((acc:EdgeMap,
                                               v:EdgeMap) => acc ++ v)
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): EdgeMap = {
    val edges = v.outgoingEdges.values
                 .foldLeft(List[String]()) { (list, e) =>
                     list ++ List(e.targetId.toString)
                 }
     Map(v.id.toString -> edges)
  }
}

class SearchAggregator(ids: List[String])
      extends AggregationOperation[(Map[String,String],Map[String,List[String]])] {
  def extract(v: Vertex[_, _]): (Map[String,String],Map[String,List[String]]) = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => (Map(),Map())
  }

  def reduce(vertices: Stream[(Map[String,String],Map[String,List[String]])]): (Map[String,String],Map[String,List[String]]) = {
    vertices.foldLeft(((Map[String,String](),Map[String,List[String]]())))((
      acc:(Map[String,String],Map[String,List[String]]), 
      v:(Map[String,String],Map[String,List[String]])) => 
        (acc._1 ++ v._1, acc._2 ++ v._2))
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): (Map[String,String],Map[String,List[String]]) = {
    if (ids.contains(v.id.toString)) {
      var nodes = Map(v.id.toString -> v.state.toString)
      val edges = v.outgoingEdges.values
         .foldLeft(List[String]()) { (list, e) =>
             nodes = nodes ++ Map(e.targetId.toString -> "unknown")
             list ++ List(e.targetId.toString)
         }
      return (nodes, Map(v.id.toString -> edges))
    }
    return ((Map[String,String](),Map[String,List[String]]()))
  }
}



