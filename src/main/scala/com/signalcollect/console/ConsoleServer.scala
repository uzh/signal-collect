/**
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
 */

package com.signalcollect.console

import scala.collection.JavaConversions._
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
import scala.reflect._
import scala.reflect.runtime.{ universe => ru }
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.ExecutionStatistics
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
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.Extraction._
import org.json4s.native.JsonMethods._
import java.io.File
import java.io.InputStream
import akka.event.Logging
import java.io.OutputStream

/** Abstract class that defines the interface for our InteractiveExecution */
abstract class Execution {
  var stepTokens: Int // How many partial steps remain until pausing
  var conditions: Map[String, BreakCondition] // Map of active conditions
  var conditionsReached: Map[String, String] // Map of fired conditions
  var state: String // Current state of the computation
  var iteration: Int // Computation iteration number
  def step: Unit // Perform a partial step
  def collect: Unit // Perform all steps until after the next collect
  def continue: Unit // Continue the computation
  def pause: Unit // Pause the computation
  def reset: Unit // Reset the graph to its initial state
  def terminate: Unit // Terminate the computation and Signal/Collect
  def addCondition(condition: BreakCondition): Unit // Add a condition
  def removeCondition(id: String): Unit // Remove a condition
}

/** Enumeration of possible break conditions */
object BreakConditionName extends Enumeration {
  type BreakConditionName = Value
  val StateChanges = Value("state changes")
  val StateAbove = Value("state above")
  val StateBelow = Value("state below")
  val SignalScoreAboveThreshold = Value("signal score above threshold")
  val SignalScoreBelowThreshold = Value("signal score below threshold")
  val CollectScoreAboveThreshold = Value("collect score above threshold")
  val CollectScoreBelowThreshold = Value("collect score below threshold")
}

import BreakConditionName._
/**
 * A break condition for the interactive execution mode
 *
 * When creating a new break condition, a number of checks are performed
 * to check whether it is valid or not. If insufficient or invalid data has
 * been provided then an IllegalArgumentException is thrown. This can occur
 * if the propsMap doesn't contain everything a break condition of a
 * particular type needs, or if the provided data is invalid. In any case,
 * the reason for the validation failure is provided in the exception.
 *
 * @constructor create a new break condition
 * @param graphConfiguration the current graph configuration
 * @param executionConfiguration the current execution configuration
 * @param name the name of the break condition as in BreakConditionName
 * @param propsMap a map of properties supplied to this break condition
 * @param workerApi a workerApi
 */
class BreakCondition(val graphConfiguration: GraphConfiguration[_, _],
  val executionConfiguration: ExecutionConfiguration,
  val name: BreakConditionName,
  val propsMap: Map[String, String],
  val workerApi: WorkerApi[_, _]) {

  val props = collection.mutable.Map(propsMap.toSeq: _*)

  // Find the graph vertex matching the vertexId which is supplied as a string
  // If the vertex does not exist, throw an exception
  require(
    if (props.contains("vertexId")) {
      props("vertexId") match {
        case id: String =>
          val result = workerApi.aggregateAll(new FindVerticesByIdsAggregator(List(props("vertexId"))))
          result.size match {
            case 0 => false
            case otherwise =>
              props += ("currentState" -> result.head.state.toString)
              true
          }
        case otherwise => false
      }
    } else {
      false
    }, "Missing or invalid vertexId!")

  require(name match {
    case StateAbove
      | StateBelow => props.contains("expectedState")
    case otherwise => true
  }, "Missing expectedState!")

  require(
    if (props.contains("expectedState")) {
      try {
        props("expectedState").toDouble
        true
      } catch {
        case e: NumberFormatException =>
          false
      }
    } else {
      true
    }, "Invalid state! Needs to be parseable as double.")

  name match {
    case SignalScoreBelowThreshold
      | SignalScoreAboveThreshold =>
      props += ("signalThreshold" -> executionConfiguration.signalThreshold.toString)
    case CollectScoreBelowThreshold
      | CollectScoreAboveThreshold =>
      props += ("collectThreshold" -> executionConfiguration.collectThreshold.toString)
    case otherwise => true
  }

}

/**
 * The main class representing the Console Server
 *
 * The ConsoleServer class sets up the WebSocket and HTTP servers needed to
 * satisfy client requests. The ports used by the HTTP server can be set in
 * the graphConfiguration using the consoleHttpPort option. The WebSocket
 * server will always use a port 100 ports above the HTTP server. If the user
 * has not supplied an option, the ConsoleServer will attempt to use the
 * default port (8080) and it will automatically try ports above that if
 * the port is already in use. This way, the ConsoleServer should always be
 * able to start.
 *
 * @constructor create a new ConsoleServer
 * @param graphConfiguration the current graph configuration
 */
class ConsoleServer[Id, Signal](graphConfiguration: GraphConfiguration[Id, Signal]) {

  // Start the HTTP and WebSocket servers on the configured port or the
  // highest available default port if none was configured by the user.
  val (server: HttpServer,
    sockets: WebSocketConsoleServer[Id, Signal]) = startServers(graphConfiguration.consoleHttpPort)

  // Things from the web-data folder will be served to the client
  server.createContext("/", new FileServer)
  server.setExecutor(Executors.newCachedThreadPool)
  server.start
  println("HTTP server started on http://localhost:" +
    server.getAddress.getPort + "")

  sockets.start
  println("WebSocket - Server started on port: " + sockets.getPort)

  /** Returns the HttpServer */
  def getServer: HttpServer = server

  /** Returns the WebSocketConsoleServer */
  def getSockets: WebSocketConsoleServer[Id, Signal] = sockets

  /**
   * Starts a new HTTP and WebSocket server, using the specified port for the
   * HTTP server if possible. Else attempts to find a pair of free ports and
   * use those.
   *
   *  @param httpPort attempt to start the HTTP server on this port
   */
  def startServers(httpPort: Int): (HttpServer, WebSocketConsoleServer[Id, Signal]) = {
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

  /**
   * Attempt to instantiate HTTP and WebSocket servers.
   *
   *  @param httpPort attempt to start the HTTP server on this port
   *  @return httpPort attempt to start the HTTP server on this port
   */
  def getNewServers(httpPort: Int): (HttpServer, WebSocketConsoleServer[Id, Signal]) = {
    val server: HttpServer =
      HttpServer.create(new InetSocketAddress(httpPort), 0)
    val sockets: WebSocketConsoleServer[Id, Signal] =
      new WebSocketConsoleServer[Id, Signal](new InetSocketAddress(httpPort + 100), graphConfiguration)
    (server, sockets)
  }

  /** Set an Actor to be the Coordinator */
  def setCoordinator(coordinatorActor: ActorRef) {
    sockets.setCoordinator(coordinatorActor)
  }

  /** Set the Execution */
  def setExecution(e: Execution) {
    sockets.setExecution(e)
  }

  /** Set the ExecutionConfiguration */
  def setExecutionConfiguration(e: ExecutionConfiguration) {
    sockets.setExecutionConfiguration(e)
  }

  /** Set the ExecutionStatistics */
  def setExecutionStatistics(e: ExecutionStatistics) {
    sockets.setExecutionStatistics(e)
  }

  /** Stop both HTTP and WebSocket servers and exit */
  def shutdown {
    println("Stopping WebSocket...")
    sockets.stop(5000)
    println("Stopping http server...")
    server.stop(5)
  }
}

/**
 * An HttpHandler to server static files from the web-data directory
 *
 * The handler automatically sets the MIME type for files ending in
 * html, css, js, png, svg and ico. Other files are assumed to be
 * plain text.
 *
 * @constructor create a new FileServer
 */
class FileServer() extends HttpHandler {

  /** Handles requests to files */
  def handle(t: HttpExchange) {

    /** The name of the file to store the log messages to */
    val logFileName = "log_messages.txt"

    /** The name of the folder where all web data is stored */
    val folderName = "web-data"

    /** The URI of the current request without the leading slash */
    var target = t.getRequestURI.getPath.replaceFirst("^[/.]*", "")

    // the root location, /graph and /resources all point to main.html
    if (List("", "graph", "resources").contains(target)) {
      target = "html/main.html"
    }
    val fileType = target match {
      case t if t.matches(".*\\.html$") => "text/html"
      case t if t.matches(".*\\.css$") => "text/css"
      case t if t.matches(".*\\.js$") => "application/javascript"
      case t if t.matches(".*\\.png$") => "image/png"
      case t if t.matches(".*\\.svg$") => "image/svg+xml"
      case t if t.matches(".*\\.ico$") => "image/x-icon"
      case otherwise => "text/plain"
    }

    /** Get the responseBody of the stream */
    def os: OutputStream = t.getResponseBody
    t.getResponseHeaders.set("Content-Type", fileType)

    // Log files are served as attachments
    if (target.endsWith(logFileName)) {
      t.getResponseHeaders.set("Content-Disposition", "attachment; filename=" + logFileName)
    }

    try {
      val root = "./src/main/resources/" + folderName
      var inputStream: InputStream = null
      // If the file exists, use it (when using a cloned repository)
      if ((new File(root)).exists()) {
        val targetPath = {
          if (target.endsWith(logFileName)) { target }
          else { root + "/" + target }
        }
        try {
          inputStream = new FileInputStream(targetPath)
        } catch {
          case e: java.io.FileNotFoundException =>
            t.sendResponseHeaders(404, 0)
            inputStream = new FileInputStream(root + "/html/404.html")
            t.getResponseHeaders.set("Content-Type", "text/html")
        }
      } // If the file doesn't exist, use the resource from the jar file
      else {
        inputStream = getClass().getClassLoader()
          .getResourceAsStream(folderName + "/" + target)
        if (inputStream == null) {
          t.sendResponseHeaders(404, 0)
          inputStream = getClass().getClassLoader()
            .getResourceAsStream(folderName + "/html/404.html")
          t.getResponseHeaders.set("Content-Type", "text/html")
        }
      }
      val file = new BufferedInputStream(inputStream.asInstanceOf[InputStream])
      t.sendResponseHeaders(200, 0)
      Iterator
        .continually(file.read)
        .takeWhile(-1 !=)
        .foreach(os.write)
      file.close
    } catch {
      case e: Exception => {
        t.getResponseHeaders.set("Content-Type", "text/plain")
        t.sendResponseHeaders(500, 0)
        os.write(("An exception occurred:\n" + e.getMessage + "\n" +
          e.getStackTrace.mkString("\n")).getBytes)
        e.printStackTrace
      }
    } finally {
      os.close
    }
  }
}

/**
 * The WebSocketServer implementation
 *
 * @constructor create a WebSocketConsoleServer
 * @param port the port to start the server on
 * @param config the current graph configuration
 */
class WebSocketConsoleServer[Id, Signal](port: InetSocketAddress, config: GraphConfiguration[Id, Signal])
  extends WebSocketServer(port) {

  // the coordinator, execution and executionConfiguration will be set at a
  // later instance, when they are ready. This is because we start the server
  // as early as possible and these things can be supplied later.
  var coordinator: Option[Coordinator[Id, Signal]] = None
  var execution: Option[Execution] = None
  var executionStatistics: Option[ExecutionStatistics] = None
  var executionConfiguration: Option[ExecutionConfiguration] = None
  var breakConditions = List()
  val graphConfiguration = config
  implicit val formats = DefaultFormats

  def setCoordinator(c: ActorRef) {
    println("ConsoleServer: got coordinator " + c)
    coordinator = Some(AkkaProxy.newInstance[Coordinator[Id, Signal]](c))
  }

  def setExecution(e: Execution) {
    execution = Some(e)
  }

  def setExecutionStatistics(e: ExecutionStatistics) {
    executionStatistics = Some(e)
  }

  def setExecutionConfiguration(e: ExecutionConfiguration) {
    executionConfiguration = Some(e)
  }

  def onError(socket: WebSocket, ex: Exception) {
    ex match {
      case e: java.nio.channels.ClosedByInterruptException =>
      case otherwise =>
        println("WebSocket - an error occured: " + ex)
        ex.printStackTrace
    }
  }

  def onMessage(socket: WebSocket, msg: String) {
    try {
      val j = parse(msg)
      val p = (j \ "provider").extract[String]
      def provider: DataProvider = coordinator match {
        case Some(c) => p match {
          case "configuration" => new ConfigurationDataProvider(this, c)
          // Temporary workaround, delivering log msgs via coordinator was not a good idea.
          case "log" => new NotReadyDataProvider(msg)
          case "graph" => new GraphDataProvider(c, j)
          case "resources" => new ResourcesDataProvider(c, j)
          case "state" => new StateDataProvider(this)
          case "controls" => new ControlsProvider(this, j)
          case "breakconditions" => new BreakConditionsProvider(c, this, j)
          case otherwise => new InvalidDataProvider(msg, "invalid provider")
        }
        case None => p match {
          case "state" => new StateDataProvider(this)
          case otherwise => new NotReadyDataProvider(msg)
        }
      }

      try {
        socket.send(compact(render(provider.fetch)))
      } catch {
        // Something went wrong during fetch, but it might be a minor exception
        case e: NumberFormatException =>
          socket.send(compact(render(
            new InvalidDataProvider(msg, "number format exception").fetch)))
        case e: Exception =>
          socket.send(compact(render(
            new ErrorDataProvider(e).fetch)))
        case e: WebsocketNotConnectedException =>
          println("Warning: Couldn't send message to websocket")
      }
    } catch {
      // Something is wrong with the WebSocket or the graph/coordinator is already terminated
      case e @ (_: java.lang.InterruptedException |
        _: akka.pattern.AskTimeoutException |
        _: org.java_websocket.exceptions.WebsocketNotConnectedException) =>
        println("Warning: Console communication interrupted " +
          "(likely due to graph shutdown)")
      case e: Exception =>
        println("Warning: Unknown exception occured")
    }
  }

  def onOpen(socket: WebSocket, handshake: ClientHandshake) {
    println("WebSocket - client connected: " +
      socket.getRemoteSocketAddress.getAddress.getHostAddress)
  }

  def onClose(socket: WebSocket, code: Int, reason: String, remote: Boolean) {
    println("WebSocket - client disconected: " +
      socket.getRemoteSocketAddress.getAddress.getHostAddress)
  }

  /** Create a current StateDataProvider message and send it to all clients */
  def updateClientState() {
    val data = new StateDataProvider(this).fetch
    val cs = connections().toList
    cs.synchronized {
      cs.foreach { c =>
        c.send(compact(render(data)))
      }
    }
  }

}

/** A tool class containing a few frequently transformation functions */
object Toolkit {
  implicit val formats = DefaultFormats

  /**
   * Yield all the properties of an object as a Map of strings to JValue.
   *
   * Using introspection, this function prepares the serialization of
   * arbitrary case classes. It will create a map of the property name to
   * whatever is contained in the property as a JValue.
   *
   * @tparam T the ClassTag of the object to be unpacked
   * @param obj the object to be unpacked
   */
  def unpackObjectToMap[T: ClassTag: ru.TypeTag](obj: T): Map[String, JValue] = {
    // find out the names of the field methods
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable
    }
    val mirror = ru.runtimeMirror(obj.getClass.getClassLoader)
    val im = mirror.reflect(obj)
    // for each field, get the value and put it in a JValue
    methods.map { m =>
      val value = Toolkit.serializeAny(im.reflectField(m.asTerm).get)
      (m.name.toString -> value)
    }.toMap
  }

  /** Wrapper yielding a JObject of the return value of unpackObjectToMap */
  def unpackObject[T: ClassTag: ru.TypeTag](obj: T): JObject = {
    val unpacked = unpackObjectToMap[T](obj)
    JObject(unpacked.map { case (k, v) => JField(k, v) }.toList)
  }

  /** Wrapper to unpack an array of objects using unpackObject */
  def unpackObjects[T: ClassTag: ru.TypeTag](obj: Array[T]): JObject = {
    val unpacked = obj.toList.map(unpackObjectToMap[T])
    JObject(unpacked.head.map {
      case (k, _) =>
        JField(k, JArray(unpacked.map { m => m(k) }))
    }.toList)
  }

  /** Merge the maps in ms using f to merge values with the same key */
  def mergeMaps[A, B](ms: Traversable[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
    }

  /** Serialize objects or collections of arbitrary type  */
  def serializeAny(o: Any): JValue = {
    try {
      o match {
        case x: Array[_] => JArray(x.toList.map(serializeAny(_)))
        case x: List[_] => JArray(x.map(serializeAny(_)))
        case x: Long => JInt(x)
        case x: Int => JInt(x)
        case x: String => JString(x)
        case x: Double if x.isNaN => JDouble(0)
        case x: Double => JDouble(x)
        case x: Map[_, _] => decompose(x)
        case x: BreakConditionName.Value => JString(x.toString)
        case other => JString(other.toString)
      }
    }
    catch { case e: Exception => { JString(o.toString) } }
  }
}

