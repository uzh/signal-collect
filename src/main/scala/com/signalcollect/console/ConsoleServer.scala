/*
 *  @author Philip Stutz
 *  
 *  Copyright 2012 University of Zurich
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

import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.util.concurrent.Executors
import akka.actor.ActorRef
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.messaging.AkkaProxy

class ConsoleServer(coordinatorActor: ActorRef, address: InetSocketAddress = new InetSocketAddress(8080)) {
  val server = HttpServer.create(address, 0)
  server.createContext("/", new CoordinatorRequestHandler(coordinatorActor))
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("Console server started on " + address.getHostName + ":" + address.getPort)

  def shutdown {
    server.stop(0)
  }
}

class CoordinatorRequestHandler(coordinatorActor: ActorRef) extends HttpHandler {
  val coordinator: Coordinator = AkkaProxy.newInstance[Coordinator](coordinatorActor)

  def handle(exchange: HttpExchange) {
    val requestMethod = exchange.getRequestMethod
    if (requestMethod.equalsIgnoreCase("GET")) {
      val responseHeaders = exchange.getResponseHeaders
      responseHeaders.set("Content-Type", "text/plain")
      exchange.sendResponseHeaders(200, 0)
      val responseBody = exchange.getResponseBody
      responseBody.write(("\nGlobal inbox size: " + coordinator.globalInboxSize).getBytes)
      responseBody.write(("\nNumber of vertices: " + coordinator.getWorkerApi.getWorkerStatistics.numberOfVertices).getBytes)
      responseBody.close
    }
  }
}