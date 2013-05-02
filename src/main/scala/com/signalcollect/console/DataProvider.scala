/*
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

// TODO: unify nodes/vertices nomenclature

package com.signalcollect.console

import java.io.StringWriter
import java.io.PrintWriter
import scala.collection.JavaConversions.propertiesAsScalaMap
import com.signalcollect.interfaces.Coordinator
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.configuration.GraphConfiguration
import com.signalcollect.interfaces.Inspectable
import com.signalcollect.TopKFinder
import com.signalcollect.SampleVertexIds
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Extraction._
import com.signalcollect.interfaces.WorkerStatistics
import com.signalcollect.interfaces.NodeStatistics
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.event.Logging.LogEvent
import akka.actor.ActorLogging

trait DataProvider {
  def fetch(): JObject
  def fetchInvalid(msg: JValue = JString(""), 
                   comment: String): JObject = {
    new InvalidDataProvider(msg, comment).fetch
  }
}

class ErrorDataProvider(e: Exception) extends DataProvider {
  def fetchStacktrace(): String = {
    val sw = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    sw.toString()
  }
  def fetch(): JObject = {
    ("provider" -> "error") ~
    ("msg" -> "An exception occured") ~
    ("stacktrace" -> fetchStacktrace())
  }
}

class InvalidDataProvider(msg: JValue, comment: String = "No comment") extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "invalid") ~
    ("msg" -> compact(render(msg))) ~
    ("comment" -> comment)
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

class StateDataProvider[Id](socket: WebSocketConsoleServer[Id])
                             extends DataProvider {
  def fetch(): JObject = {
    val reply: JObject = socket.execution match {
      case Some(e) => 
        ("state" -> e.state) ~ 
        ("steps" -> e.steps) ~ 
        ("iteration" -> e.iteration)
      case None => ("state" -> "non-interactive")
    }
    ("provider" -> "state") ~ reply
  }
}


class ConfigurationDataProvider[Id](socket: WebSocketConsoleServer[Id],
                              coordinator: Coordinator[Id, _],
                              msg: JValue) extends DataProvider {
  def fetch(): JObject = {
    val executionConfiguration = socket.executionConfiguration match {
      case Some(e: ExecutionConfiguration) => Toolkit.unpackObjects(Array(e))
      case otherwise => JObject(List(JField("unknown", "unknown")))
    }
    ("provider" -> "configuration") ~ 
    ("executionConfiguration" -> executionConfiguration) ~
    ("graphConfiguration" -> Toolkit.unpackObjects(Array(socket.graphConfiguration))) ~
    ("systemProperties" -> propertiesAsScalaMap(System.getProperties()))
  }
}

class LogDataProvider[Id](coordinator: Coordinator[Id, _]) extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "log") ~ 
    ("messages" -> coordinator.getLogMessages.map(_.toString)) 
  }
}

case class ControlsRequest(
  control: Option[String]
)

class ControlsProvider(socket: WebSocketConsoleServer[_],
                           msg: JValue) extends DataProvider {

  implicit val formats = DefaultFormats
  var execution: Option[Execution] = socket.execution

  def command(e: Execution, command: String): JObject = { 
    command match {
      case "step" => e.step
      case "collect" => e.collect
      case "pause" => e.pause
      case "continue" => e.continue
      case "reset" => e.reset
      case "terminate" => e.terminate
    }
    ("msg" -> "command accepted")
  }

  def fetch(): JObject = {
    val request = (msg).extract[ControlsRequest]
    val reply = execution match {
      case Some(e) => request.control match {
        case Some(action) => action match {
          case "step" | "collect" | "pause" | "continue" | "reset" | "terminate" =>
            command(e, action)
          case otherwise => fetchInvalid(msg, "invalid control!")
        }
        case None => fetchInvalid(msg, "missing command!")
      }
      case None => fetchInvalid(msg, "interactive execution is unavailable!")
    }
    ("provider" -> "controls") ~
    reply
  }
}

case class BreakConditionsRequest(
  action: Option[String],
  name: Option[String],
  id: Option[String],
  props: Option[Map[String,String]]
)
case class BreakConditionContainer(
  id: String,
  name: String,
  props: Map[String,String]
)

class BreakConditionsProvider[Id](coordinator: Coordinator[Id, _], 
                                  socket: WebSocketConsoleServer[Id],
                                  msg: JValue) extends DataProvider {

  implicit val formats = DefaultFormats
  var execution: Option[Execution] = socket.execution
  val workerApi = coordinator.getWorkerApi 

  def fetchConditions(e: Execution): JObject = {
    val active = e.conditions.map { case (id, c) =>
      Toolkit.unpackObject(BreakConditionContainer(id, c.name.toString, c.props.toMap))
    }.toList
    val reached = decompose(e.conditionsReached)
    ("provider" -> "breakconditions") ~
    ("active" -> active) ~
    ("reached" -> reached)
  }

  def fetch(): JObject = {
    execution match {
      case Some(e) => 
        // add or remove conditions
        val request = (msg).extract[BreakConditionsRequest]
        request.action match {
          case Some(action) => action match {
            case "add" => request.name match {
              case Some(name) => 
                try {
                  val n = BreakConditionName.withName(name)
                  request.props match {
                    case Some(props) => 
                      socket.executionConfiguration match {
                        case Some(c) =>
                          try {
                            val condition = new BreakCondition(socket.graphConfiguration, 
                                                               c, n, props, workerApi)
                            e.addCondition(condition)
                            fetchConditions(e)
                          }
                          catch { case e: IllegalArgumentException => 
                            fetchInvalid(msg, new ErrorDataProvider(e).fetchStacktrace())
                          } 
                        case None => fetchInvalid(msg, "executionConfiguration unavailable!")
                      }
                    case None => fetchInvalid(msg, "missing props!")
                  }
                }
                catch { case e: NoSuchElementException =>
                  fetchInvalid(msg, "invalid Name!")
                }
              case None => fetchInvalid(msg, "missing name!")
            }
            case "remove" => request.id match {
              case Some(id) => 
                e.removeCondition(id)
                fetchConditions(e)
              case None => fetchInvalid(msg, "Missing id!")
            }
          }
          case None => fetchConditions(e)
        }
        case None => 
          ("provider" -> "breakconditions") ~
          ("status" -> "noExecution")
    }
  }
}

case class GraphDataRequest(
  nodeIds: Option[List[String]],
  vicinityRadius: Option[Int],
  vicinityIncoming: Option[Boolean],
  query: Option[String], 
  targetCount: Option[Int],
  topCriterium: Option[String]
)

class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue) 
                            extends DataProvider {

  implicit val formats = DefaultFormats

  val workerApi = coordinator.getWorkerApi 
  var nodeIds = List[String]()
  var targetCount = 5
  var vicinityRadius = 0
  var vicinityIncoming = false

  def findVicinity(vertexIds: Set[Id], radius: Int = 3, 
                   incoming: Boolean = false): Set[Id] = {
    if (radius == 0) { vertexIds }
    else {
      if (incoming) {
        val nodes = workerApi.aggregateAll(new FindNodeVicinitiesByIdsAggregator[Id](vertexIds))
        vertexIds ++ findVicinity(nodes, radius - 1, true)
      }
      else {
        vertexIds ++ findVicinity(vertexIds.map { id =>
          workerApi.forVertexWithId(id, { vertex: Inspectable[Id,_] =>
            vertex.getTargetIdsOfOutgoingEdges.map(_.asInstanceOf[Id]).toSet
          })
        }.flatten, radius - 1, false)
      }
    }
  }

  def fetchGraph(nodes: Set[Id] = Set[Id]()): JObject = {
    val nodesOfIds = workerApi.aggregateAll(
                     new FindVerticesByIdsAggregator[Id](nodeIds))
    val vicinity = findVicinity(nodes ++ nodesOfIds.map { _.id }.toSet, 
                                vicinityRadius, vicinityIncoming)
    workerApi.aggregateAll(new GraphAggregator[Id](vicinity))
  }

  def fetchTopState(inverted: Boolean = false): JObject = {
    val topState = workerApi.aggregateAll(new TopStateAggregator[Id](targetCount, inverted)).take(targetCount)
    val nodes = topState.foldLeft(Set[Id]()){ (acc, m) => acc + m._2 }
    fetchGraph(nodes)
  }

  def fetchTopDegree(): JObject = {
    val nodes = workerApi.aggregateAll(new TopDegreeAggregator[Id](targetCount))
                             .toSeq.sortBy(-_._2)
                             .take(targetCount)
                             .map{ _._1 }
                             .toSet
    fetchGraph(nodes)
  }

  def fetchTopScore(scoreType: String): JObject = {
    val topScore = workerApi.aggregateAll(new TopScoreAggregator[Id](targetCount, scoreType)).take(targetCount)
    val nodes = topScore.foldLeft(Set[Id]()){ (acc, m) => acc + m._2 }
    fetchGraph(nodes)
  }

  def fetchSample(): JObject = {
    val nodes = workerApi.aggregateAll(new SampleAggregator[Id](targetCount))
    fetchGraph(nodes)
  }

  def fetch(): JObject = {
    val request = (msg).extract[GraphDataRequest]
    request.nodeIds match {
      case Some(ids) => nodeIds = ids
      case otherwise => 
    }
    request.targetCount match {
      case Some(t) => targetCount = t
      case otherwise => 
    }
    request.vicinityRadius match {
      case Some(r) => vicinityRadius = r
      case otherwise => 
    }
    request.vicinityIncoming match {
      case Some(b) => vicinityIncoming = b
      case otherwise => 
    }
    val graphData = request.query match {
      case Some("nodeIds") => request.nodeIds match {
        case Some(ids) => fetchGraph()
        case otherwise => fetchInvalid(msg, "missing nodeIds")
      }
      case Some("top") => request.topCriterium match {
        case Some("Highest State") => fetchTopState()
        case Some("Lowest State") => fetchTopState(true)
        case Some("Highest degree") => fetchTopDegree()
        case Some("Signal score") => fetchTopScore("signal")
        case Some("Collect score") => fetchTopScore("collect")
        case otherwise => new InvalidDataProvider(msg).fetch
      }
      case otherwise => fetchInvalid(msg, "missing query")
    }
    
    ("provider" -> "graph") ~
    graphData
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _], msg: JValue)
      extends DataProvider {


  def fetch(): JObject = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: Array[WorkerStatistics] = 
      (coordinator.getWorkerApi.getIndividualWorkerStatistics).toArray
    val wstats = Toolkit.unpackObjects(ws)
    val ns: Array[NodeStatistics] = 
      (coordinator.getWorkerApi.getIndividualNodeStatistics).toArray
    val nstats = Toolkit.unpackObjects(ns)
    
    ("provider" -> "resources") ~
    ("timestamp" -> System.currentTimeMillis) ~
    ("inboxSize" -> inboxSize) ~
    ("workerStatistics" -> wstats) ~
    ("nodeStatistics" -> nstats)
  }
}

