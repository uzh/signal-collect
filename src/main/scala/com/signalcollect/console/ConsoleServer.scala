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
import sjson.json._
import sjson.json.DefaultProtocol._
import sjson.json.JsonSerialization._

class ConsoleServer(
              httpPort: InetSocketAddress = new InetSocketAddress(8080)) {

  val server = HttpServer.create(httpPort, 0)
  server.createContext("/", new FileServer("web-data"))
  server.createContext("/api", new ApiServer())
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("HTTP server started on localhost:" + httpPort.getPort)

  val socketsPort = new InetSocketAddress(httpPort.getPort() + 1)
  val sockets = new WebSocketConsoleServer(socketsPort);
  sockets.start();
  System.out.println( "WebSocket server started on port: " + sockets.getPort() );

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
      tojson(Map("provider" -> "invalid",
                 "msg"      -> ("Received an invalid message: " + msg))
            ).toString

    }
}

class NotReadyDataProvider(msg: String) extends DataProvider {
    def fetch(): String = {
      tojson(Map("provider" -> "notready",
                 "msg"      -> "The signal/collect computation is not ready yet",
                 "request"  -> msg)
            ).toString
    }
}

class GraphDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
    val workerApi = coordinator.getWorkerApi 
    val vertexAggregator = new VertexToStringAggregator
    val edgeAggregator = new EdgeToStringAggregator
    val content = new StringBuilder()
    def fetch(): String = {
      val vertices = workerApi.aggregateAll(vertexAggregator)
      val edges = workerApi.aggregateAll(edgeAggregator)
      case class GraphData(vertices: Map[String,String], 
                           edges: Map[String,List[String]],
                           provider: String = "graph")
      implicit val GraphDataFormat: Format[GraphData] = 
                   asProduct3("nodes", "edges", "provider")(
                              GraphData)(GraphData.unapply(_).get)
      val data = GraphData(vertices, edges)
      tojson(data).toString
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
  def fetch(): String = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val workerStatistics: List[WorkerStatistics] = 
      (coordinator.getWorkerApi.getIndividualWorkerStatistics)

    val workerStatisticsHeaders = List(
          "messagesSent", 
          "workerId",
          "messagesReceived",
          "toSignalSize",
          "toCollectSize",
          "collectOperationsExecuted",
          "signalOperationsExecuted",
          "numberOfVertices",
          "verticesAdded",
          "verticesRemoved",
          "numberOfOutgoingEdges",
          "outgoingEdgesAdded",
          "outgoingEdgesRemoved",
          "receiveTimeoutMessagesReceived",
          "heartbeatMessagesReceived",
          "signalMessagesReceived",
          "bulkSignalMessagesReceived",
          "continueMessagesReceived",
          "requestMessagesReceived",
          "otherMessagesReceived"
    )

    val workerStatisticsTempMap: 
        scala.collection.mutable.Map[String,List[Long]] = 
        scala.collection.mutable.Map[String,List[Long]]()

    workerStatisticsHeaders.foreach { h =>
      var l: List[Long] = List() 
      workerStatistics.foreach { w =>
        val srcVal: Any = w.getClass.getMethods
                           .find(_.getName == h).get.invoke(w)
        val dstVal: Long = srcVal match {
          case l: Long => l
          case al: Array[Long] => al.sum.toLong
          case i: Int => i.toLong
          case other => 0
        }
        l = dstVal :: l
      }
      workerStatisticsTempMap += (h -> l.reverse)
    }

    val workerStatisticsMap = workerStatisticsTempMap.toMap

    val systemInformation: List[SystemInformation] = 
      (coordinator.getWorkerApi.getIndividualSystemInformation)

    implicit val SystemInformationFormat: Format[SystemInformation] = 
                 asProduct14("workerId", "os", "runtime_mem_total", 
                             "runtime_mem_max", "runtime_mem_free", 
                             "runtime_cores", "jmx_committed_vms", 
                             "jmx_mem_free", "jmx_mem_total",
                             "jmx_swap_free", "jmx_swap_total", 
                             "jmx_process_load", "jmx_process_time", 
                             "jmx_system_load")(
                            SystemInformation)(SystemInformation.unapply(_).get)

    case class ResourcesData(inboxSize: Long, 
                             workerStatistics: Map[String,List[Long]],
                             systemStatistics: List[SystemInformation],
                             provider: String = "resources",
                             timestamp : Long = System.currentTimeMillis)
    implicit val ResourcesDataFormat: Format[ResourcesData] = 
                 asProduct5("inboxSize", "workerStatistics", "systemStatistics", 
                            "provider", "timestamp")(
                            ResourcesData)(ResourcesData.unapply(_).get)

    val data = ResourcesData(inboxSize, workerStatisticsMap, systemInformation)
    tojson(data).toString
    
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

