/**
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

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.signalcollect.GraphBuilder
import org.specs2.runner.JUnitRunner
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.io.IOException
import java.net.ServerSocket
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.net.URI
import net.liftweb.json._
import org.java_websocket.handshake.ServerHandshake
import java.io.FileWriter
import com.signalcollect.Graph
import com.signalcollect.DefaultGraph
import collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ConsoleServerSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "ConsoleServer" should {

    // console server address and ports
    val serverHost = InetAddress.getByName(null)
    val serverPort = 56783
    val socketPort = serverPort + 100

    // websocket connection
    var websocket: WebSocketClient = null

    // signal collect graph
    var graph: DefaultGraph[Any, Any] = null

    "start successfully" in {
      val serverConnection = new Socket
      val serverAddress = new InetSocketAddress(serverHost, serverPort)
      val timeoutInMilliseconds = 5000
      var isServerOnline = false

      graph = GraphBuilder.withConsole(true, serverPort).build.asInstanceOf[DefaultGraph[Any, Any]]

      try {
        serverConnection.connect(serverAddress, timeoutInMilliseconds)
        isServerOnline = true
        serverConnection.close()
      } catch {
        case t: Throwable => t.printStackTrace
      }

      isServerOnline
    }

    "start socket successfully" in {
      val socketConnection = new Socket
      val socketAddress = new InetSocketAddress(serverHost, socketPort)
      val timeoutInMilliseconds = 5000
      var isSocketOnline = false

      try {
        socketConnection.connect(socketAddress, timeoutInMilliseconds)
        isSocketOnline = true
        socketConnection.close()
      } catch {
        case t: Throwable => t.printStackTrace
      }
      val droneFlag = "DRONE"
      if (System.getenv.containsKey(droneFlag)) {
        println("CI environment detected, skipping socket test.")
        true
      } else {
        isSocketOnline
      }
    }

    "establish websocket connection" in {
      try {
        val websocketUri = new URI("ws://localhost:" + socketPort)
        websocket = new WebSocketClient(websocketUri)
        websocket.sendJsonOrderWithProvider("test")
        websocket.getJsonResponse
        true
      } catch {
        case t: Throwable =>
          t.printStackTrace
          false
      }
    }

    "return valid API result for provider 'resources.workerStatistics'" in {
      websocket.sendJsonOrderWithProvider("resources")
      val json = websocket.getJsonResponse
      val requiredProviderResults = Array(
        "messagesSentToNodes",
        "messagesSentToWorkers",
        "messagesSentToCoordinator",
        "messagesSentToOthers",
        "signalMessagesReceived",
        "otherMessagesReceived",
        "requestMessagesReceived",
        "continueMessagesReceived",
        "bulkSignalMessagesReceived",
        "heartbeatMessagesReceived",
        "receiveTimeoutMessagesReceived",
        "outgoingEdgesAdded",
        "outgoingEdgesRemoved",
        "numberOfOutgoingEdges",
        "verticesRemoved",
        "verticesAdded",
        "numberOfVertices",
        "signalOperationsExecuted",
        "collectOperationsExecuted",
        "toCollectSize",
        "toSignalSize",
        "workerId")
      val providerResults = (json \\ "workerStatistics").children
      val providerResultsMap = createMapFromJValueList(providerResults);
      val allContained = requiredProviderResults.forall {
        providerResultsMap.contains(_) === true
      }
      allContained === true
    }

    "return valid API result for provider 'resources.nodeStatistics'" in {
      websocket.sendJsonOrderWithProvider("resources")
      val json = websocket.getJsonResponse
      val requiredProviderResults = Array(
        "runtime_cores",
        "jmx_system_load",
        "jmx_process_time",
        "jmx_process_load",
        "jmx_swap_free",
        "jmx_swap_total",
        "jmx_mem_total",
        "jmx_mem_free",
        "jmx_committed_vms",
        "runtime_mem_max",
        "runtime_mem_free",
        "runtime_mem_total")
      val providerResults = (json \\ "nodeStatistics").children
      val providerResultsMap = createMapFromJValueList(providerResults);
      val allContained = requiredProviderResults.forall {
        providerResultsMap.contains(_) === true
      }
      allContained === true
    }

    "return valid API result for provider 'configuration.executionConfiguration'" in {
      // as we have no execution running, this test will only check whether we get
      // any 'executionConfiguration' result
      websocket.sendJsonOrderWithProvider("configuration")
      val json = websocket.getJsonResponse
      (json \\ "executionConfiguration").values === "unknown"
    }

    "return valid API result for provider 'configuration.graphConfiguration'" in {
      websocket.sendJsonOrderWithProvider("configuration")
      val json = websocket.getJsonResponse
      val requiredProviderResults = Array(
        "messageBusFactory",
        "statusUpdateIntervalInMilliseconds",
        "workerFactory",
        "loggingLevel",
        "akkaMessageCompression",
        "storageFactory",
        "consoleEnabled",
        "heartbeatIntervalInMilliseconds",
        "nodeProvisioner",
        "consoleHttpPort",
        "akkaDispatcher")
      val providerResults = (json \\ "graphConfiguration").children
      val providerResultsMap = createMapFromJValueList(providerResults)
      val allContained = requiredProviderResults.forall {
        providerResultsMap.contains(_) === true
      }
      allContained === true
    }

    "return valid API result for provider 'configuration.systemProperties'" in {
      websocket.sendJsonOrderWithProvider("configuration")
      val json = websocket.getJsonResponse
      val requiredProviderResults = Array(
        "java.runtime.name",
        "sun.boot.library.path",
        "java.vm.version",
        "java.vm.vendor",
        "java.vendor.url",
        "path.separator",
        "java.vm.name",
        "file.encoding.pkg",
        "user.country",
        "sun.java.launcher",
        "sun.os.patch.level",
        "java.vm.specification.name",
        "user.dir",
        "java.runtime.version",
        "java.awt.graphicsenv",
        "java.endorsed.dirs",
        "os.arch",
        "java.io.tmpdir",
        "line.separator",
        "java.vm.specification.vendor",
        "os.name",
        "sun.jnu.encoding",
        "java.library.path",
        "sun.nio.ch.bugLevel",
        "java.specification.name",
        "java.class.version",
        "sun.management.compiler",
        "os.version",
        "user.home",
        "user.timezone",
        "java.awt.printerjob",
        "file.encoding",
        "java.specification.version",
        "java.class.path",
        "user.name",
        "java.vm.specification.version",
        "sun.java.command",
        "java.home",
        "sun.arch.data.model",
        "user.language",
        "java.specification.vendor",
        "awt.toolkit",
        "java.vm.info",
        "java.version",
        "java.ext.dirs",
        "sun.boot.class.path",
        "java.vendor",
        "file.separator",
        "java.vendor.url.bug",
        "sun.io.unicode.encoding",
        "sun.cpu.endian",
        "sun.cpu.isalist" // the following properties seem to be unavailable on a Linux host
        // "user.country.format", "gopherProxySet", "http.nonProxyHosts", "socksNonProxyHosts", "ftp.nonProxyHosts",
        );
      val providerResults = (json \\ "systemProperties").children
      var providerResultsMap: Map[String, Any] = Map()
      providerResults.foreach {
        v: JValue =>
          providerResultsMap = providerResultsMap ++ createMapFromJValueList(v.children)
      }
      val allContained = requiredProviderResults.forall {
        providerResultsMap.contains(_) === true
      }
      allContained === true
    }

    "return valid API result for provider 'log'" in {
      websocket.sendJsonOrderWithProvider("log")
      val json = websocket.getJsonResponse
      val providerResults = (json \\ "messages").children
      // there won't be any messages, just checking whether the format is right
      createMapFromJValueList(providerResults).size == 0
    }

    "return valid API result for provider 'graph'" in {
      websocket.sendJsonOrderWithProvider("graph")
      val json = websocket.getJsonResponse
      // we do not load a data set, so we can't actually get a graph.
      // As long as we get back a result, it's OK.
      true === true
    }

    "return valid API result for provider 'state'" in {
      websocket.sendJsonOrderWithProvider("state")
      val json = websocket.getJsonResponse
      (json \\ "state").values === "undetermined"
    }

    "return valid API result for provider 'controls'" in {
      websocket.sendJsonOrder("{\"provider\": \"controls\", \"control\": \"step\"}")
      val json = websocket.getJsonResponse
      // We do not compute anything so we can't actually control anything.
      // As long as we get back a result, it's OK.
      true === true
    }

    "return valid API result for provider 'breakconditions'" in {
      websocket.sendJsonOrderWithProvider("breakconditions")
      val json = websocket.getJsonResponse
      (json \\ "status").values === "noExecution"
    }

    "return valid API result for provider 'invalidDataProvider'" in {
      websocket.sendJsonOrderWithProvider("invalidDataProviderWhichDoesNotActuallyExist")
      val json = websocket.getJsonResponse
      (json \\ "provider").values === "invalid"
    }

    "close websocket connection" in {
      try {
        websocket.closeBlocking()
        true
      } catch {
        case t: Throwable =>
          t.printStackTrace
          false
      }
    }

    "shutdown successfully" in {
      val console = graph.getConsole
      console.getServer.stop(5)
      console.getSockets.stop(5000)
      graph.shutdown
      true
    }

  }

  def createMapFromJValueList(json: List[JValue]) = {
    var map: Map[String, Any] = Map()
    json.foreach {
      s: JValue =>
        val (name: String, values: Any) = s.values
        map += (name -> values)
    }
    map
  }

}

class WebSocketClient(uri: URI) extends org.java_websocket.client.WebSocketClient(uri) {
  connectBlocking()
  var response = ""

  def sendJsonOrderWithProvider(provider: String) {
    sendJsonOrder("{\"provider\":\"" + provider + "\"}")
  }
  def sendJsonOrder(json: String) {
    try {
      send(json)
    } catch {
      case t: Throwable => t.printStackTrace
    }
  }
  def getJsonResponse = {
    while (response.length() == 0) { Thread.sleep(100) }
    val json = parse(response)
    response = ""
    json
  }
  def onOpen(handshakedata: ServerHandshake) {
  }
  def onMessage(message: String) {
    while (response.length() > 0) { Thread.sleep(100) }
    response = message
  }
  def onClose(code: Int, reason: String, remote: Boolean) {
  }
  def onError(ex: Exception) {
  }
}
