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
                                      Inspectable }
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

class ConsoleServer(coordinatorActor: ActorRef,
              httpPort: InetSocketAddress = new InetSocketAddress(8080),
              bindIp: String = InetAddress.getLocalHost.getHostAddress) {
  val socketsPort = new InetSocketAddress(httpPort.getPort() + 1)
  def sockets = new WebSocketConsoleServer(socketsPort, coordinatorActor);
  sockets.start();
  System.out.println( "WebSocket server started on port: " + sockets.getPort() );

  val server = HttpServer.create(httpPort, 0)

  server.createContext("/", new FileServer("main.html"))
  server.createContext("/graph", new FileServer("main.html"))
  server.createContext("/resources", new FileServer("main.html"))
  server.createContext("/main.js", 
                       new FileServer("main.js", "application/javascript"))
  server.createContext("/main.css", 
                       new FileServer("main.css", "text/css"))
  server.createContext("/graph.js", 
                       new FileServer("graph.js", "application/javascript"))
  server.createContext("/graph.css", 
                       new FileServer("graph.css", "text/css"))
  server.createContext("/resources.js", 
                       new FileServer("resources.js", "application/javascript"))
  server.createContext("/sc.png", 
                       new FileServer("sc.png", "image/png"))
  server.createContext("/rickshaw.min.js", 
                       new FileServer("rickshaw-1.2.0.min.js", 
                                      "application/javascript"))
  server.createContext("/reconnecting-websocket.min.js", 
                       new FileServer("reconnecting-websocket.min.js", 
                                      "application/javascript"))

  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("HTTP server started on localhost:" + httpPort.getPort)

  def shutdown {
    server.stop(0)
    sockets.stop(0)
  }
}

class FileServer(fileName: String,
                 fileType: String = "text/html") extends HttpHandler {
  def handle(t: HttpExchange) {
    t.getResponseHeaders.set("Content-Type", fileType)
    t.sendResponseHeaders(200, 0)
    def root = "./web-data/";
    def os = t.getResponseBody; 
    val file = new BufferedInputStream(new FileInputStream(root + fileName))
    Iterator 
      .continually (file.read)
      .takeWhile (-1 !=)
      .foreach (os.write)
    file.close
    os.close
  }
}

class WebSocketConsoleServer(port: InetSocketAddress,
                             coordinatorActor: ActorRef)
                             extends WebSocketServer(port) {
  val coordinator: Coordinator[_, _] = 
                   AkkaProxy.newInstance[Coordinator[_, _]](coordinatorActor)

  def onError(socket: WebSocket, ex: Exception) {
    println("WebSocket - an error occured: " + ex)
  }

  def onMessage(socket: WebSocket, msg: String) {
    def provider: DataProvider = msg match {
      case "graph" => new GraphDataProvider(coordinator)
      case "resources" => new ResourcesDataProvider(coordinator)
      case otherwise => new InvalidDataProvider()
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

class InvalidDataProvider extends DataProvider {
    def fetch(): String = "{\"msg\":\"invalid request\"}"
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
                           edges: Map[String,Map[String,String]],
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

    case class ResourcesData(inboxSize: Long, 
                             workerStatistics: Map[String,List[Long]],
                             provider: String = "resources")
    implicit val ResourcesDataFormat: Format[ResourcesData] = 
                 asProduct3("inboxSize", "workerStatistics", "provider")(
                            ResourcesData)(ResourcesData.unapply(_).get)

    val data = ResourcesData(inboxSize, workerStatisticsMap)
    tojson(data).toString
  }
}

class VertexToStringAggregator extends AggregationOperation[Map[String, String]] {
  def extract(v: Vertex[_, _]): Map[String,String] = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => Map()
  }

  def reduce(vertices: Stream[Map[String,String]]): Map[String,String] = {
    vertices reduce (_ ++ _)
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): Map[String,String] = {
    Map(v.id.toString -> v.state.toString)
  }
}

class EdgeToStringAggregator 
      extends AggregationOperation[Map[String,Map[String,String]]] {
  def extract(v: Vertex[_, _]): Map[String,Map[String,String]] = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
  }

  def reduce(vertices: Stream[Map[String,Map[String,String]]]): 
                              Map[String,Map[String,String]] = {
    vertices reduce (_ ++ _)
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): Map[String,Map[String,String]] = {
    val edges = v.outgoingEdges.values
                 .foldLeft(Map[String,Map[String,String]]()) { (map, e) =>
                     map ++ Map(e.id.toString ->
                                Map("source" -> v.id.toString, 
                                    "target" -> e.targetId.toString))
                 }
    edges
  }
}

