/*
 *  @author Philip Stutz
 *  @author Carol Alexandru
 *  @author Silvan Troxler
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

import akka.actor.Actor
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
import com.signalcollect.interfaces.WorkerStatus
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
import java.io.File
import java.io.InputStream

trait Execution {
  var steps: Int
  def step()
  def continue()
  def pause()
  def reset()
  def terminate()
}
class ConsoleServer[Id](userHttpPort: Int) {

  val (server: HttpServer, 
       sockets: WebSocketConsoleServer[Id]) = setupUserPorts(userHttpPort)
  
  server.createContext("/", new FileServer("web-data"))
  server.setExecutor(Executors.newCachedThreadPool)
  server.start
  println("HTTP server started on http://localhost:" + 
          server.getAddress.getPort + "")

  sockets.start
  println("WebSocket - Server started on port: " + sockets.getPort)

  def setupUserPorts(httpPort: Int): 
      (HttpServer, WebSocketConsoleServer[Id]) = {
    val minAllowedUserPortNumber = 1025
    if (httpPort < minAllowedUserPortNumber) {
      val defaultPort = 8080
      val maxUserPort = 8179
      println("Websocket - No valid port given (using default port " + 
              defaultPort + ")")
      for (port <- defaultPort to maxUserPort) {
        try {
          println("Websocket - Connecting to port " + port + "...")
          return getNewServers(port)
        } catch {
          case e: Exception => 
            println("Websocket - Starting server on port " + 
                    port + " failed: " + e.getMessage)
        }
      }
      println("Could not start server on ports " + defaultPort + 
              " to " + maxUserPort)
      sys.exit
    } else {
      try {
        return getNewServers(httpPort)
      } catch {
        case e: Throwable => 
          println("Could not start server: " + e.getMessage)
          sys.exit
      }
    }
  }
  
  def getNewServers(httpPort: Int) = {
    val server: HttpServer = 
        HttpServer.create(new InetSocketAddress(httpPort), 0)
    val sockets: WebSocketConsoleServer[Id] = 
        new WebSocketConsoleServer[Id](new InetSocketAddress(httpPort + 100))
    (server, sockets)
  }
  
  def setCoordinator(coordinatorActor: ActorRef) = {
    sockets.setCoordinator(coordinatorActor)
  }

  def setExecution(e: Execution) = {
    sockets.setExecution(e)
  }

  def shutdown = {
    println("Stopping http server...")
    server.stop(5)
    println("Stopping WebSocket...")
    sockets.stop(5000)
    sys.exit
  }
}

class FileServer(folderName: String) extends HttpHandler {
  def handle(t: HttpExchange) {

    var target = t.getRequestURI.getPath.replaceFirst("^[/.]*", "") 
    if (List("", "graph", "resources").contains(target)) { 
      target = "main.html"
    }
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
      val root = "./src/main/resources/" + folderName
      var inputStream : InputStream = null
      if ((new File(root)).exists()) {
        // read from the filesystem
        inputStream = new FileInputStream(root + "/" + target)
      } else {
        // read from the JAR
        inputStream = ClassLoader.getSystemResource(folderName + "/" + target).openStream()
      }
      val file = new BufferedInputStream(inputStream.asInstanceOf[InputStream])
      t.getResponseHeaders.set("Content-Type", fileType)
      t.sendResponseHeaders(200, 0)
      Iterator 
        .continually (file.read)
        .takeWhile (-1 !=)
        .foreach (os.write)
      file.close
    }
    catch {
      case e: Exception => {
        t.getResponseHeaders.set("Content-Type", "text/plain")
        t.sendResponseHeaders(200, 0)
        os.write(("An exception occurred:\n" + e.getMessage() + "\n" + e.getStackTraceString).getBytes())
        e.printStackTrace()
      }
    }
    finally {
      os.close
    }
  }
}

class WebSocketConsoleServer[Id](port: InetSocketAddress)
                             extends WebSocketServer(port) {
  var coordinator: Option[Coordinator[Id,_]] = None
  var execution: Option[Execution] = None
  implicit val formats = DefaultFormats

  def setCoordinator(c: ActorRef) {
    println("ConsoleServer: got coordinator " + c)
    coordinator = Some(AkkaProxy.newInstance[Coordinator[Id, _]] (c))
  }

  def setExecution(e: Execution) {
    execution = Some(e)
  }

  def onError(socket: WebSocket, ex: Exception) {
    println("WebSocket - an error occured: " + ex)
    ex.printStackTrace
  }

  def onMessage(socket: WebSocket, msg: String) {
    val j = parse(msg)
    val p = (j \ "provider").extract[String]
    def provider: DataProvider = coordinator match {
      case Some(c) => p match {
        case "graph" => new GraphDataProvider[Id](c, j)
        case "resources" => new ResourcesDataProvider(c, j)
        case "status" => new StatusDataProvider(this)
        case "api" => new ApiProvider[Id](this, j)
        case otherwise => new InvalidDataProvider(msg)
      }
      case None => p match{
        case "status" => new StatusDataProvider(this)
        case otherwise => new NotReadyDataProvider(msg)
      }
    }
    socket.send(compact(render(provider.fetch)))
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
  def fetch(): JObject
  def fetchInvalid(msg: JValue = JString("")): JObject = {
    new InvalidDataProvider(compact(render(msg))).fetch
  }
}

class InvalidDataProvider(msg: String) extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "invalid") ~
    ("msg" -> ("Received an invalid message: " + msg))
  }
}

class NotReadyDataProvider(msg: String) extends DataProvider {
  implicit val formats = DefaultFormats
  val j = parse(msg)
  val p = (j \ "provider").extract[String]
  def fetch(): JObject = {
    ("provider" -> "notready") ~
    ("targetProvider" -> p) ~
    ("msg" -> "The signal/collect computation is not ready yet") ~
    ("request" -> msg)
  }
}

class StatusDataProvider[Id](socket: WebSocketConsoleServer[Id])
                             extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "status") ~
    ("interactive" -> (socket.execution match {
      case None => false
      case otherwise => true
    }))
  }
}

case class ApiRequest(
  provider: String, 
  control: Option[String]
)

class ApiProvider[Id](socket: WebSocketConsoleServer[Id],
                      msg: JValue) extends DataProvider {

  implicit val formats = DefaultFormats
  var execution: Option[Execution] = socket.execution

  def computationStep(e: Execution): JObject = { 
    e.step
    ("state" -> "stepping") 
  }
  def computationPause(e: Execution): JObject = {
    e.pause
    ("state" -> "pausing") 
  }
  def computationContinue(e: Execution): JObject = {
    e.continue
    ("state" -> "continuing") 
  }
  def computationReset(e: Execution): JObject = {
    e.reset
    ("state" -> "resetting") 
  }
  def computationTerminate(e: Execution): JObject = {
    e.terminate
    ("state" -> "terminating") 
  }

  def fetch(): JObject = {
    val request = (msg).extract[ApiRequest]
    val reply = execution match {
      case Some(e) => request.control match {
        case Some(action) => action match {
          case "step" => computationStep(e)
          case "pause" => computationPause(e)
          case "continue" => computationContinue(e)
          case "reset" => computationReset(e)
          case "terminate" => computationTerminate(e)
          case otherwise => fetchInvalid(msg)
        }
        case None => fetchInvalid(msg)
      }
      case None => fetchInvalid(msg)
    }
    ("provider" -> "controls") ~ reply
  }
}


case class GraphDataRequest(
  provider: String, 
  search: Option[String], 
  vicinity: Option[String],
  topk: Option[Int]
)

class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue) 
                            extends DataProvider {

  implicit val formats = DefaultFormats

  val workerApi = coordinator.getWorkerApi 

  def findVicinity(vertexIds: List[Id], depth: Int = 3): List[Id] = {
    if (depth == 0) { vertexIds }
    else {
      findVicinity(vertexIds.map { id =>
        workerApi.forVertexWithId(id, { vertex: Inspectable[Id,_] =>
          vertex.getTargetIdsOfOutgoingEdges.map(_.asInstanceOf[Id]).toList
        })
      }.flatten, depth - 1)
    }
  }

  def fetchVicinity(id: String): JObject = {
    val vertex = workerApi.aggregateAll(
                 new FindVertexByIdAggregator[Id](id))
    val vicinity = vertex match {
      case Some(v) => 
        findVicinity(List(v.id))
      case None => List[Id]()
    }
    workerApi.aggregateAll(new GraphAggregator[Id](vicinity))
  }

  def fetchTopk(n: Int): JObject = {
    val topk = new TopKFinder[Int](n)
    val nodes = workerApi.aggregateAll(topk)
    workerApi.aggregateAll(new GraphAggregator(nodes.toList.map(_._1)))
  }

  def fetchAll(): JObject = {
    workerApi.aggregateAll(new GraphAggregator)
  }

  def fetch(): JObject = {
    val request = (msg).extract[GraphDataRequest]
    val graphData = request.search match {
      case Some("vicinity") => request.vicinity match {
        case Some(id) => fetchVicinity(id)
        case otherwise => fetchInvalid(msg)
      }
      case Some("topk") => request.topk match {
        case Some(n) => fetchTopk(n)
        case otherwise => new InvalidDataProvider(compact(render(msg))).fetch
      }
      case otherwise => fetchAll
    }
    
    ("provider" -> "graph") ~
    graphData
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _], msg: JValue)
      extends DataProvider {

  def unpackObjectList[T: ClassTag: ru.TypeTag](obj: Array[T]): List[JField] = {
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable 
    }
    methods.map { m =>
      val mirror = ru.runtimeMirror(obj.head.getClass.getClassLoader)
      val values = obj.toList.map { o =>
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

  def fetch(): JObject = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: Array[WorkerStatus] = 
      (coordinator.getWorkerStatus)
    val wstats = unpackObjectList(ws.map(_.workerStatistics))
    val sstats = unpackObjectList(ws.map(_.systemInformation))

    ("provider" -> "resources") ~
    ("timestamp" -> System.currentTimeMillis) ~
    ("inboxSize" -> inboxSize) ~
    ("workerStatistics" -> JObject(wstats) ~ JObject(sstats))
  }
}

class GraphAggregator[Id](all: Boolean, ids: List[Id])
      extends AggregationOperation[JObject] {

  def this() = this(true, List[Id]())
  def this(ids: List[Id]) = this(false, ids)

  def extract(v: Vertex[_, _]): JObject = v match {
    case i: Inspectable[Id, _] => {
      if (all || ids.contains(i.id)) {
        val edges = i.outgoingEdges.values.filter { 
          v => all || ids.contains(v.targetId)
        }
        JObject(List(
          JField("nodes", JObject(List(JField(i.id.toString, i.state.toString)))),
          JField("edges", JObject(List(JField(i.id.toString, JArray(
            edges.map{ e => ( JString(e.targetId.toString))}.toList)))))
        ))
      }
      else { JObject(List()) }
    }
    case other => JObject(List())
  }

  def reduce(vertices: Stream[JObject]): JObject = {
    vertices.foldLeft(JObject(List())) { (acc, v) => 
      acc merge v
    }
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

