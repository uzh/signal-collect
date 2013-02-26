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
    //sockets.stop(0)
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
    def provider: DataProvider = coordinator match {
      case Some(c) => msg match {
        case "graph" => new GraphDataProvider(c)
        case "resources" => new ResourcesDataProvider(c)
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

class GraphDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
  val workerApi = coordinator.getWorkerApi 
  val vertexAggregator = new VertexToStringAggregator
  val edgeAggregator = new EdgeToStringAggregator
  val content = new StringBuilder()
  def fetch(): String = {
    val graphData = (
      ("provider" -> "graph") ~
      ("vertices" -> workerApi.aggregateAll(vertexAggregator)) ~
      ("edges" -> workerApi.aggregateAll(edgeAggregator)) 
    )
    return compact(render(graphData))
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
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
        ("workerId" -> List(ws.map(_.workerId))) ~
        ("messagesSent" -> List(ws.map(_.messagesSent.toList))) ~
        ("messagesReceived" -> List(ws.map(_.messagesReceived))) ~
        ("toSignalSize" -> List(ws.map(_.toSignalSize))) ~
        ("toCollectSize" -> List(ws.map(_.toCollectSize))) ~
        ("collectOperationsExecuted" -> List(ws.map(_.collectOperationsExecuted))) ~
        ("signalOperationsExecuted" -> List(ws.map(_.signalOperationsExecuted))) ~
        ("numberOfVertices" -> List(ws.map(_.numberOfVertices))) ~
        ("verticesAdded" -> List(ws.map(_.verticesAdded))) ~
        ("verticesRemoved" -> List(ws.map(_.verticesRemoved))) ~
        ("numberOfOutgoingEdges" -> List(ws.map(_.numberOfOutgoingEdges))) ~
        ("outgoingEdgesAdded" -> List(ws.map(_.outgoingEdgesAdded))) ~
        ("outgoingEdgesRemoved" -> List(ws.map(_.outgoingEdgesRemoved))) ~
        ("receiveTimeoutMessagesReceived" -> List(ws.map(_.receiveTimeoutMessagesReceived))) ~
        ("heartbeatMessagesReceived" -> List(ws.map(_.heartbeatMessagesReceived))) ~
        ("signalMessagesReceived" -> List(ws.map(_.signalMessagesReceived))) ~
        ("bulkSignalMessagesReceived" -> List(ws.map(_.bulkSignalMessagesReceived))) ~
        ("continueMessagesReceived" -> List(ws.map(_.continueMessagesReceived))) ~
        ("requestMessagesReceived" -> List(ws.map(_.requestMessagesReceived))) ~
        ("otherMessagesReceived" -> List(ws.map(_.otherMessagesReceived))) ~
        /* from SystemInformation */
        ("os" -> List(si.map(_.os))) ~
        ("runtime_mem_total" -> List(si.map(_.runtime_mem_total))) ~
        ("runtime_mem_max" -> List(si.map(_.runtime_mem_max))) ~
        ("runtime_mem_free" -> List(si.map(_.runtime_mem_free))) ~
        ("runtime_cores" -> List(si.map(_.runtime_cores))) ~
        ("jmx_committed_vms" -> List(si.map(_.jmx_committed_vms))) ~
        ("jmx_mem_free" -> List(si.map(_.jmx_mem_free))) ~
        ("jmx_mem_total" -> List(si.map(_.jmx_mem_total))) ~
        ("jmx_swap_free" -> List(si.map(_.jmx_swap_free))) ~
        ("jmx_swap_total" ->List(si.map(_.jmx_swap_total))) ~
        ("jmx_process_load" -> List(si.map(_.jmx_process_load))) ~
        ("jmx_process_time" -> List(si.map(_.jmx_process_time))) ~
        ("jmx_system_load" -> List(si.map(_.jmx_system_load)))) 
    )
    return compact(render(resourceData))

    /* code kept for reference; use something like this to genericise?
    val srcVal: Any = w.getClass.getMethods.find(_.getName == h).get.invoke(w)
    */

  }
}

class VertexToStringAggregator extends AggregationOperation[Map[String, String]] {
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

class EdgeToStringAggregator
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

