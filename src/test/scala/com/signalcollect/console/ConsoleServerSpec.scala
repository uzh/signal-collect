/*
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

@RunWith(classOf[JUnitRunner])
class ConsoleServerSpec extends SpecificationWithJUnit with Mockito {

  sequential
  
  
  "ConsoleServer" should {

    // server vals
    val serverHost = InetAddress.getByName(null)
    val serverPort = 8086
    val server = new Socket

    // socket vals
    val socketPort = serverPort + 100
    val socket = new Socket
    
    // websocket var
    var websocket : WebSocketClient = null

    

    "start successfully" in {
      val serverAddress = new InetSocketAddress(serverHost, serverPort)
      val timeoutInMilliseconds = 5000
      var isServerOnline = false

      val graph = GraphBuilder.withConsole(true, serverPort).build
      
      try {
        server.connect(serverAddress, timeoutInMilliseconds)
        isServerOnline = true
      } catch {
        case _ : Throwable => 
      }

      isServerOnline
    }

    
    
    "start socket successfully" in {
      val socketAddress = new InetSocketAddress(serverHost, socketPort)
      val timeoutInMilliseconds = 5000
      var isSocketOnline = false

      try {
        socket.connect(socketAddress, timeoutInMilliseconds)
        isSocketOnline = true
      } catch {
        case _ : Throwable => 
      }

      isSocketOnline
    }
    
    
    
    "establish websocket connection" in {
      try {
        val websocketUri = new URI("ws://localhost:" + socketPort)
        websocket = new WebSocketClient(websocketUri)
        websocket.sendJsonOrderWithProvider("test")
        websocket.getJsonResponse
        true        
      } catch {
        case _ : Throwable => false
      }
    }
    
    
    
    "return valid API result for provider 'resources'" in {
      websocket.sendJsonOrderWithProvider("resources")
      val json = websocket.getJsonResponse
      val requiredResourceResults = Array(
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
        "workerId",
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
        "runtime_mem_total"    
      );

      val workerStatistics = (json \\ "workerStatistics").children
      val map = createMapFromJValueList(workerStatistics);
      requiredResourceResults.foreach {
        res =>
          map.contains(res) === true
      }
    }
    
    
    
    "close websocket connection" in {
      try {
        websocket.close()
        true        
      } catch { case _ : Throwable => false }
    }
  
  
    
    "stop successfully" in {
      println("Client shutting down...") 
      try {
        server.close();
        true
      } catch { case _ : Throwable => false }
    }
    
    
    
    "stop socket successfully" in {
       try {
         socket.close();
         true
       } catch { case _ : Throwable => false }
    }
  }
  
  
  
  def createMapFromJValueList(json : List[JValue]) = {
    var map: Map[String, List[Any]] = Map()
    json.foreach {
      s: JValue =>
        val (name: String, values: List[Any]) = s.values
        map += (name -> values)
    }
    map
  }

}

class WebSocketClient(uri : URI) extends org.java_websocket.client.WebSocketClient(uri) {
  connectBlocking()
  var response = ""
  
  def sendJsonOrderWithProvider(provider : String) {
    val json = "{\"provider\":\"" + provider + "\"}"
    println("Client sending: " + json)
    try {
      send(json)
    } catch {
      case _ : Throwable => 
    }
  }
  def getJsonResponse = {
    while (response.length() == 0) { Thread.sleep(100) }
    val json = parse(response)
    response = ""
    json
  }
  def onOpen(handshakedata : ServerHandshake) {
    println("Client onOpen")
  }
  def onMessage(message : String) {
    println("Client received: " + message)
    while (response.length() > 0) { Thread.sleep(100) }
    response = message
  }
  def onClose(code : Int, reason : String, remote : Boolean) {
    println("Client onClose")
  }
  def onError(ex : Exception) {
    println("Client onError")
  }
}
