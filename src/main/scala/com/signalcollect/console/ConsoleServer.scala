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
    val j = parse(msg)
    val p = (j \ "provider").extract[String]
    def provider: DataProvider = coordinator match {
      case Some(c) => p match {
        case "configuration" => new ConfigurationDataProvider(this, c, msg)
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

object Toolkit {
  def unpackObject[T: ClassTag: ru.TypeTag](obj: Array[T]): JObject = {
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable 
    }
    JObject(methods.map { m =>
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
          case other => JString(other.toString)
        }
      }
      JField(m.name.toString, values)
    }.toList)
  }
  def mergeMaps[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
  }
}

