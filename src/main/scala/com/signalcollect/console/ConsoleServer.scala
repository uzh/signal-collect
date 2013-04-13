/*
 *  @author Philip Stutz
 *  @author Carol Alexandru
 *  @author Silvan Troxler
 *  
 *  Copyright 2013 University of Zurich
 *      
 *  Licensed below the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed below the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations below the License.
 *  
 */

package com.signalcollect.console

import scala.collection.JavaConversions._
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
import scala.reflect._
import scala.reflect.runtime.{universe => ru}
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.interfaces.WorkerApi
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
import net.liftweb.json.Extraction._
import java.io.File
import java.io.InputStream
import akka.event.Logging

trait Execution {
  var steps: Int
  var conditions: Map[String,BreakCondition]
  var conditionsReached: Map[String,String]
  def step()
  def continue()
  def pause()
  def reset()
  def terminate()
  def addCondition(condition: BreakCondition)
  def removeCondition(id: String)
}

object BreakConditionName extends Enumeration {
  type BreakConditionName = Value
  val ChangesState = Value("changes state")
  val GoesAboveState = Value("goes above state")
  val GoesBelowState = Value("goes below state")
  val GoesAboveSignalThreshold = Value("goes above signal threshold") 
  val GoesBelowSignalThreshold = Value("goes below signal threshold") 
  val GoesAboveCollectThreshold = Value("goes above collect threshold")
  val GoesBelowCollectThreshold = Value("goes below collect threshold") 
}

import BreakConditionName._
class BreakCondition(val graphConfiguration: GraphConfiguration,
                     val executionConfiguration: ExecutionConfiguration,
                     val name: BreakConditionName, 
                     val propsMap: Map[String,String], 
                     workerApi: WorkerApi[_,_]) {

  val props = collection.mutable.Map(propsMap.toSeq: _*)

  require( 
    if (props.contains("nodeId")) {
      props("nodeId") match {
        case id: String =>
          val result = workerApi.aggregateAll(new FindVertexByIdAggregator(props("nodeId")))
          result match {
            case Some(v) =>
              props += ("currentState" -> v.state.toString)
              true
            case None => false
          }
        case otherwise => false
      }
    }
    else { 
      false 
    }, "Missing or invalid nodeId!")

  require(name match { 
      case GoesAboveState
         | GoesBelowState  => props.contains("expectedState")
    case otherwise => true
    }, "Missing expectedState")

  name match { 
    case GoesBelowSignalThreshold
       | GoesAboveSignalThreshold =>
      props += ("signalThreshold" -> executionConfiguration.signalThreshold.toString)
    case GoesBelowCollectThreshold
       | GoesAboveCollectThreshold => 
      props += ("collectThreshold" -> executionConfiguration.collectThreshold.toString)
    case otherwise => true
  }

}

class ConsoleServer[Id](graphConfiguration: GraphConfiguration) {

  val (server: HttpServer, 
       sockets: WebSocketConsoleServer[Id]) = setupUserPorts(graphConfiguration.consoleHttpPort)

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
        new WebSocketConsoleServer[Id](new InetSocketAddress(httpPort + 100), graphConfiguration)
    (server, sockets)
  }
  
  def setCoordinator(coordinatorActor: ActorRef) = {
    sockets.setCoordinator(coordinatorActor)
  }

  def setInteractor(e: Execution) = {
    sockets.setExecution(e)
  }

  def setExecutionConfiguration(e: ExecutionConfiguration) = {
    sockets.setExecutionConfiguration(e)
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
        os.write(("An exception occurred:\n" + e.getMessage() + "\n" + 
                  e.getStackTraceString).getBytes())
        e.printStackTrace()
      }
    }
    finally {
      os.close
    }
  }
}

class WebSocketConsoleServer[Id](port: InetSocketAddress, config: GraphConfiguration)
                             extends WebSocketServer(port) {
  var coordinator: Option[Coordinator[Id,_]] = None
  var execution: Option[Execution] = None
  var executionConfiguration: Option[ExecutionConfiguration] = None
  var breakConditions = List()
  val graphConfiguration = config
  implicit val formats = DefaultFormats

  def setCoordinator(c: ActorRef) {
    println("ConsoleServer: got coordinator " + c)
    coordinator = Some(AkkaProxy.newInstance[Coordinator[Id, _]] (c))
  }

  def setExecution(e: Execution) {
    execution = Some(e)
  }

  def setExecutionConfiguration(e: ExecutionConfiguration) {
    executionConfiguration = Some(e)
  }

  def onError(socket: WebSocket, ex: Exception) {
    println("WebSocket - an error occured: " + ex)
    ex.printStackTrace
  }

  def onMessage(socket: WebSocket, msg: String) {
//    if (coordinator.isDefined) println(coordinator.get.getLogMessages(Logging.DebugLevel, 10))
    val j = parse(msg)
    val p = (j \ "provider").extract[String]
    def provider: DataProvider = coordinator match {
      case Some(c) => p match {
        case "configuration" => new ConfigurationDataProvider(this, c, msg)
        case "log" => new LogDataProvider(c)
        case "graph" => new GraphDataProvider[Id](c, j)
        case "resources" => new ResourcesDataProvider(c, j)
        case "status" => new StatusDataProvider(this)
        case "controls" => new ControlsProvider[Id](this, j)
        case "breakconditions" => new BreakConditionsProvider[Id](c, this, j)
        case otherwise => new InvalidDataProvider(msg)
      }
      case None => p match{
        case "status" => new StatusDataProvider(this)
        case otherwise => new NotReadyDataProvider(msg)
      }
    }

    try {
      socket.send(compact(render(provider.fetch)))
    } 
    catch {
      case e: NumberFormatException =>
        socket.send(compact(render(new InvalidDataProvider(msg).fetch)))
      case e: Exception =>
        socket.send(compact(render(new ErrorDataProvider(e).fetch)))
    } 
  }

  def onOpen(socket: WebSocket, handshake:ClientHandshake) {
    println("WebSocket - client connected: " + 
            socket.getRemoteSocketAddress.getAddress.getHostAddress)
  }

  def onClose(socket: WebSocket, code: Int, reason: String, remote: Boolean) {
    println("WebSocket - client disconected: " + 
            socket.getRemoteSocketAddress.getAddress.getHostAddress)
  }

  def sendMsg(msg: JObject) {
    val cs = connections().toList
    cs.synchronized {
      cs.foreach { c =>
        c.send(compact(render(msg)));
      }
    }
  }

}

object Toolkit {
  implicit val formats = DefaultFormats
  def unpackObjectToMap[T: ClassTag: ru.TypeTag](obj: T): Map[String,JValue] = {
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable 
    }
    val mirror = ru.runtimeMirror(obj.getClass.getClassLoader)
    val im = mirror.reflect(obj)
    methods.map { m =>
      val value = im.reflectField(m.asTerm).get match {
        case x: Array[Long] => JArray(x.toList.map(JInt(_)))
        case x: Long => JInt(x)
        case x: Int => JInt(x)
        case x: String => JString(x)
        case x: Double if x.isNaN => JDouble(0)
        case x: Double => JDouble(0)
        case x: Map[_,_] => decompose(x)
        case x: BreakConditionName.Value => JString(x.toString)
        case other => JString(other.toString)
      }
      (m.name.toString -> value)
    }.toMap
  }
  def unpackObject[T: ClassTag: ru.TypeTag](obj: T): JObject = {
    val unpacked = unpackObjectToMap(obj)
    JObject(unpacked.map { case (k, v) => JField(k, v) }.toList)
  }
  def unpackObjects[T: ClassTag: ru.TypeTag](obj: Array[T]): JObject = {
    val unpacked = obj.toList.map(unpackObjectToMap)
    JObject(unpacked.head.map { case (k, _) =>
      JField(k, JArray(unpacked.map{ m => m(k) })) 
    }.toList)
  }
  def mergeMaps[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
  }

}

