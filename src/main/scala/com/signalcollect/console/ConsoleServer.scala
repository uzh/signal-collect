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

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import scala.language.postfixOps
import scala.Array.canBuildFrom
import scala.collection.immutable.List.apply
import com.signalcollect.interfaces.{ Coordinator, WorkerStatistics }
import com.signalcollect.messaging.AkkaProxy
import com.sun.net.httpserver.{ HttpExchange, HttpHandler, HttpServer }
import akka.actor.ActorRef

class ConsoleServer(coordinatorActor: ActorRef, address: InetSocketAddress = new InetSocketAddress(8080)) {
  val server = HttpServer.create(address, 0)
  server.createContext("/", new WorkerStateRequestHandler(coordinatorActor))
  server.createContext("/inspect", new GraphInspectorRequestHandler(coordinatorActor))
  server.setExecutor(Executors.newCachedThreadPool())
  server.start
  println("Console server started on localhost:" + address.getPort)

  def shutdown {
    server.stop(0)
  }
}

class GraphInspectorRequestHandler(coordinatorActor: ActorRef) extends HttpHandler {
  val coordinator: Coordinator[_, _] = AkkaProxy.newInstance[Coordinator[_, _]](coordinatorActor)

  def handle(exchange: HttpExchange) {
    val requestMethod = exchange.getRequestMethod
    if (requestMethod.equalsIgnoreCase("GET")) {
      val responseHeaders = exchange.getResponseHeaders
      responseHeaders.set("Content-Type", "text/html")
      exchange.sendResponseHeaders(200, 0)
      val responseBody = exchange.getResponseBody
      responseBody.write(renderInspector.getBytes)
      responseBody.close
    }
  }
  
  def renderInspector: String = {
    val content = new StringBuffer()
    content.append("Hello, Ela! This is where we're gonna put the graph visualization")

    //http://sigmajs.org/data/les_miserables.gexf
    
    
    val sigmaExample = """
<div id="sigma-instance" style="display:block;margin-left:auto;margin-right:auto;width:80%;height:800px;border:1px #ccf solid;"></div>
<script type="text/javascript">
function init() {
  // Instantiate sigma.js and customize it :
  var sigInst = sigma.init(document.getElementById('sigma-instance'));
sigInst.addNode('hello',{'label': 'Hello', 'x': 100, 'y': 100, 'size': 4.5, 'color' : 'rgb(100, 10, 20)'})
sigInst.addNode('world',{'label': 'World!', 'x': 120, 'y': 130, 'size': 4.5, 'color' : 'rgb(10, 100, 20)'})
sigInst.addEdge('hello-world', 'hello','world');


  document.getElementById('rescale-graph').addEventListener('click',function(){
    sigInst.position(0,0,1).draw();
  },true);
}

if (document.addEventListener) {
  document.addEventListener('DOMContentLoaded', init, false);
} else {
  window.onload = init;
}
</script>
      """
      content.append(sigmaExample)
    Template.html(title = "Signal/Collect Inspector", content = content.toString)
  }
}

class WorkerStateRequestHandler(coordinatorActor: ActorRef) extends HttpHandler {
  val coordinator: Coordinator[_, _] = AkkaProxy.newInstance[Coordinator[_, _]](coordinatorActor)

  def handle(exchange: HttpExchange) {
    val requestMethod = exchange.getRequestMethod
    if (requestMethod.equalsIgnoreCase("GET")) {
      val responseHeaders = exchange.getResponseHeaders
      responseHeaders.set("Content-Type", "text/html")
      exchange.sendResponseHeaders(200, 0)
      val responseBody = exchange.getResponseBody
      responseBody.write(renderConsole.getBytes)
      responseBody.close
    }
  }

  def renderConsole: String = {
    val content = new StringBuffer()

    // Global inbox size
    val inboxSize = Template.bodyCopy("Global inbox size: " + coordinator.getGlobalInboxSize.toString)
    val inboxSizeElement = Template.gridRow(span = 4, offset = 2, content = inboxSize)
    content.append(inboxSizeElement)

    // Actor stats
    val workerStatistics: List[WorkerStatistics] = coordinator.getWorkerApi.getIndividualWorkerStatistics
    val senderStats: List[String] = coordinator.getWorkerApi.getWorkerStatistics.messagesSent map (_.toString) toList
    val workerNames: List[String] = (0 until senderStats.length - 2) map ("Worker " + _) toList
    val tableHeaders = List(
      "workerId",
      "toSignalSize",
      "toCollectSize",
      "messagesSent",
      "messagesReceived",
      "collectOperationsExecuted",
      "signalOperationsExecuted",
      "numberOfVertices",
      "outgoingEdges",
      "receiveTimeoutMessagesReceived",
      "heartbeatMessagesReceived",
      "signalMessagesReceived",
      "bulkSignalMessagesReceived",
      "continueMessagesReceived",
      "requestMessagesReceived",
      "otherMessagesReceived"
    )

    val rowEntries = workerStatistics map (stats =>
      List(stats.workerId,
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
        stats.otherMessagesReceived
      ) map (_.toString)
    )

    val workerStatsTable = Template.table(tableHeaders, rowEntries)
    val actorSendingStatsElement = Template.gridRow(span = 4, offset = 2, content = workerStatsTable)
    content.append(actorSendingStatsElement)

    Template.html(title = "Signal/Collect Console", content = content.toString, refreshIntervalSeconds = Some(1))
  }
}

object Template extends App {

  def bodyCopy(text: String): String = s"""
<p>$text</p>
"""

  def leadBodyCopy(text: String): String = s"""
<p class="lead">$text</p>
"""

  def gridRow(span: Int, offset: Int, content: String) = s"""
<div class="row">
  <div class="span${span} offset${offset}">
    $content
  </div>
</div>
"""

  def table(titles: List[String], tableRows: List[List[String]]) = s"""
<table class="table table-striped">
  ${tableHeaders(titles)}
  <tbody>
  ${tableRows map tableRow mkString}
  </tbody>
</table>
"""

  def tableRow(tr: List[String]) = s"""
<tr>${tr map tableCell mkString}</tr>
"""
  def tableCell(value: String) = s"""
<td>$value</td>
"""

  def tableHeaders(titles: List[String]) = s"""
<thead>
  <tr>
    ${titles map tableHeader mkString}
  </tr>
</thead>
"""

  def tableHeader(title: String) = s"""
<th>$title</th>
"""

  def html(title: String, content: String, refreshIntervalSeconds: Option[Int] = None) = s"""
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
    ${refreshIntervalSeconds match {
      case None => ""
      case Some(seconds) => s"""<meta http-equiv="refresh" content="$seconds" >"""
    }}
    <title>$title</title>
    <link rel="stylesheet" type="text/css" href="http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.0.4/css/bootstrap.min.css"/>
    <script type="text/javascript" src="http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.1.1/bootstrap.min.js"></script>
    <script type="text/javascript" src="http://sigmajs.org/js/sigma.min.js"></script>
    <script type="text/javascript" src="http://sigmajs.org/js/sigma.parseGexf.js"></script>
  </head>
  <body>
    <section class="content">
      $content
    </section>
  </body>
</html>
"""
}