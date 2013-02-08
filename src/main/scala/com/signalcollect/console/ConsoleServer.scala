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
import com.signalcollect.interfaces.{ Coordinator, WorkerStatistics, Inspectable }
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

class ConsoleServer(coordinatorActor: ActorRef,
              httpPort: InetSocketAddress = new InetSocketAddress(8080),
              socketsPort: InetSocketAddress = new InetSocketAddress(8081),
              bindIp: String = InetAddress.getLocalHost.getHostAddress) {
  def sockets = new WebSocketConsoleServer(socketsPort, coordinatorActor);
  sockets.start();
  System.out.println( "WebSocket server started on port: " + sockets.getPort() );

  val server = HttpServer.create(httpPort, 0)
  server.createContext("/", new FileServer("graph.html"))
  server.createContext("/graph", new FileServer("graph.html"))
  server.createContext("/graph.js", new FileServer("graph.js", "application/javascript"))
  server.createContext("/rickshaw.min.js", new FileServer("rickshaw-1.2.0.min.js", "application/javascript"))
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("HTTP server started on localhost:" + httpPort.getPort)

  def shutdown {
    server.stop(0)
    sockets.stop(0)
  }
}

class FileServer(fileName: String = "index.html",
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

class WebSocketConsoleServer(
         port: InetSocketAddress,
         coordinatorActor: ActorRef)
      extends WebSocketServer(port) {
  val coordinator: Coordinator[_, _] = AkkaProxy.newInstance[Coordinator[_, _]](coordinatorActor)

  def onError(socket: WebSocket, ex: Exception): Unit = ???

  def onMessage(socket: WebSocket, msg: String) {
    def provider: DataProvider = msg match {
      case "graph" => new GraphDataProvider(coordinator)
      case "computation" => new ComputationDataProvider(coordinator)
      case "resources" => new ResourceDataProvider(coordinator)
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

class GraphDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
    val workerApi = coordinator.getWorkerApi 
    val vertexAggregator = new VertexToStringAggregator
    val edgeAggregator = new EdgeToStringAggregator
    val content = new StringBuilder()
    def fetch(): String = {
      val vertices = workerApi.aggregateAll(vertexAggregator)
      val edges = workerApi.aggregateAll(edgeAggregator)
      content.append("{\"vertices\":[" + vertices + "],\"edges\":[" + edges + "]}")
      content.toString
  }
}

class ComputationDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
    def fetch(): String = {
      "stub"
    }
}

class ResourceDataProvider(coordinator: Coordinator[_, _]) extends DataProvider {
  def fetch(): String = {
    val content = new StringBuilder()

    val inboxSize = coordinator.getGlobalInboxSize.toString
    content.append("{\"inboxSize\":" + inboxSize)

    val senderStats: List[String] =
        (coordinator.getWorkerApi.getWorkerStatistics.messagesSent map (_.toString) toList)
    content.append(",\"msgsSent\":[" + senderStats.mkString(",") + "]")

    val workerStatistics: List[WorkerStatistics] = 
        (coordinator.getWorkerApi.getIndividualWorkerStatistics)
    content.append(",\"workerStats\":[")

    val wstats = workerStatistics map {stats =>
      val statsContent = new StringBuilder()
      statsContent.append("[")
      def s = List(stats.workerId,
                   stats.toSignalSize,
                   stats.toCollectSize,
                   stats.messagesSent.sum,
                   stats.messagesReceived,
                   stats.collectOperationsExecuted,
                   stats.signalOperationsExecuted,
                   stats.numberOfVertices,
                   stats.outgoingEdgesAdded - stats.outgoingEdgesRemoved,
                   stats.receiveTimeoutMessagesReceived,
                   stats.heartbeatMessagesReceived,
                   stats.signalMessagesReceived,
                   stats.bulkSignalMessagesReceived,
                   stats.continueMessagesReceived,
                   stats.requestMessagesReceived,
                   stats.otherMessagesReceived) map (_.toString)
       statsContent.append(s.mkString(","))
       statsContent.append("]")
       statsContent.toString
    }
    content.append(wstats.mkString(",") + "]}")
    content.toString
  }
}

class VertexToStringAggregator extends AggregationOperation[String] {
  def extract(v: Vertex[_, _]): String = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => ""
  }

  def reduce(vertices: Stream[String]): String = {
    vertices.mkString(",")
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): String = {
    "\"" + v.id.toString + "\""
  }
}

class EdgeToStringAggregator extends AggregationOperation[String] {
  def extract(v: Vertex[_, _]): String = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => ""
  }

  def reduce(vertices: Stream[String]): String = {
    vertices.mkString(",")
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): String = {
    val edges = v.getTargetIdsOfOutgoingEdges.foldLeft(List[String]()) { (list, e) =>
      ("(\"" + v.id.toString + "\",\"" + e.toString + "\")") :: list
    }
    edges.mkString(",")
  }
}

