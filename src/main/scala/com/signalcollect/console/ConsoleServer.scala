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
import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.language.postfixOps
import scala.reflect._ 
import scala.reflect.runtime.{universe => ru}
import scala.util.Random

import com.signalcollect.interfaces.AggregationOperation
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.Inspectable
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.interfaces.SystemInformation
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.TopKFinder
import com.signalcollect.Vertex
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer

import akka.actor.ActorRef

import org.java_websocket._
import org.java_websocket.WebSocketImpl
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

class ConsoleServer[Id](userHttpPort: Int) {

  val (server: HttpServer, sockets: WebSocketConsoleServer[Id]) = setupUserPorts(userHttpPort)
  
  server.createContext("/", new FileServer("web-data"))
  server.createContext("/api", new ApiServer())
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("HTTP server started on http://localhost:" + server.getAddress().getPort() + "")

  sockets.start();
  println("WebSocket - Server started on port: " + sockets.getPort())

  def setupUserPorts(httpPort: Int) : (HttpServer, WebSocketConsoleServer[Id]) = {
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
    val sockets: WebSocketConsoleServer[Id] = new WebSocketConsoleServer[Id](new InetSocketAddress(httpPort + 100));
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

class WebSocketConsoleServer[Id](port: InetSocketAddress)
                             extends WebSocketServer(port) {
  var coordinator: Option[Coordinator[Id,_]] = None;

  def setCoordinator(coordinatorActor: ActorRef) {
    println("ConsoleServer: got coordinator ActorRef")
    coordinator = Some(AkkaProxy.newInstance[Coordinator[Id, _]]
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
        case "graph" => new GraphDataProvider[Id](c, j)
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
class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue) 
      extends DataProvider {
  implicit val formats = DefaultFormats
  val workerApi = coordinator.getWorkerApi 
  def fetch(): String = {
    val request = (msg).extract[GraphDataRequest]
    println(request)
    val graphData = request.search match {
      case Some("vicinity") => request.id match {
        case Some(id) =>
          var vertexAggregator = new FindVertexByIdAggregator[Id](id)
          var result = workerApi.aggregateAll(vertexAggregator)
          var nodes = result match {
            case Some(vertex) => 
              println(vertex)
              println(vertex.id)
              workerApi.forVertexWithId(vertex.id, { i: Inspectable[Id,_] =>
                  (i.id, i.outgoingEdges.values.foldLeft(List[String]()){
                    (list, e) => e.targetId.toString :: list
                  })
                })
            case None => List[String]()
          }
          println(nodes)
          ("provider" -> "graph") ~
          ("nodes" -> "") ~
          ("edges" -> "")
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

  def unpackObjectList[T: ClassTag: ru.TypeTag](obj: List[T]): List[JField] = {
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable 
    }
    methods.map { m =>
      val mirror = ru.runtimeMirror(obj.head.getClass.getClassLoader)
      val values = obj.map { o =>
        val im = mirror.reflect(o)
        im.reflectField(m.asTerm).get match {
          case x: Array[Long] => JArray(x.toList.map(JInt(_)))
          case x: Long => JInt(x)
          case x: Int => JInt(x)
          case x: String => JString(x)
          case x: Double if x.isNaN => JDouble(0)
          case x: Double => JDouble(0)
        }
      }
      JField(m.name.toString, values)
    }.toList
  }

  def fetch(): String = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: List[WorkerStatistics] = 
      (coordinator.getWorkerApi.getIndividualWorkerStatistics)
    val wstats = unpackObjectList(ws)

    var si: List[SystemInformation] = 
      (coordinator.getWorkerApi.getIndividualSystemInformation)
    val sstats = unpackObjectList(si)

    val resourceData = (
      ("provider" -> "resources") ~
      ("timestamp" -> System.currentTimeMillis) ~
      ("inboxSize" -> inboxSize) ~
      ("workerStatistics" -> JObject(wstats) ~ JObject(sstats))
    )
    compact(render(resourceData))
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
  type NodeMap = (Map[String,String],Map[String,List[String]])

  def extract(v: Vertex[_, _]): NodeMap = v match {
    case i: Inspectable[_, _] => vertexToSigmaAddCommand(i)
    case other => (Map(),Map())
  }

  def reduce(vertices: Stream[NodeMap]): NodeMap = {
    vertices.foldLeft((Map[String,String](),Map[String,List[String]]()))(
      (acc:NodeMap, 
      v:NodeMap) => 
        (acc._1 ++ v._1, acc._2 ++ v._2))
  }

  def vertexToSigmaAddCommand(v: Inspectable[_, _]): NodeMap = {
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

class FindVertexByIdAggregator[Id](id: String)
      extends AggregationOperation[Option[Vertex[Id,_]]] {
  def extract(v: Vertex[_, _]): Option[Vertex[Id,_]] = v match {
    case i: Inspectable[Id, _] => {
      if (i.id.toString == id) { return Some(i) }
      else { return None }
    }
    case other => None
  }

  def reduce(vertices: Stream[Option[Vertex[Id,_]]]): Option[Vertex[Id,_]] = {
    vertices.flatten.headOption
  }

}



