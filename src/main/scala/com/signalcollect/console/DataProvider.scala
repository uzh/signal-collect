/**
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
import com.signalcollect.ExecutionStatistics
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.event.Logging.LogEvent
import akka.actor.ActorLogging

/** The trait defining the interface every DataProvider has to implement. */
trait DataProvider {
  def fetch(): JObject
  def fetchInvalid(msg: JValue = JString(""),
                   comment: String): JObject = {
    new InvalidDataProvider(msg, comment).fetch
  }
}

/**
 * DataProvider that wraps a stack trace.
 *
 * @constructor create a new ErrorDataProvider
 * @param e the exception that has been thrown
 */
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

/**
 * DataProvider used when invalid communication occurs.
 *
 * When an invalid request is received, this provider is used to supply the
 * original message that caused the problem plus a comment that could further
 * explain the problem.
 *
 * @constructor create a new InvalidDataProvider
 * @param msg the original request that caused the problem
 * @param comment more information on why the request is invalid
 */
class InvalidDataProvider(msg: JValue, comment: String = "No comment") extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "invalid") ~
      ("msg" -> compact(render(msg))) ~
      ("comment" -> comment)
  }
}

/**
 * DataProvider used when another data provider can't satisfy the request.
 *
 * It can occur that a valid request cannot be satisfied, for example because
 * the underlying data is not available yet. In that case, this data provider
 * is used to inform the client of the problem
 *
 * @constructor create a new NotReadyDataProvider
 * @param msg the original request
 */
class NotReadyDataProvider(msg: String) extends DataProvider {
  implicit val formats = DefaultFormats
  val j = parse(msg)
  val p = (j \ "requestor").extract[String]
  def fetch(): JObject = {
    ("provider" -> "notready") ~
      ("requestor" -> p) ~
      ("msg" -> "The signal/collect computation is not ready yet") ~
      ("request" -> msg)
  }
}

/**
 * DataProvider used to fetch the current state of the computation.
 *
 * @constructor create a new StateDataProvider
 * @param socket the WebSocketConsoleServer
 */
class StateDataProvider[Id](socket: WebSocketConsoleServer[Id])
    extends DataProvider {
  def fetch(): JObject = {
    val reply: JObject = socket.execution match {
      case Some(e) =>
        ("state" -> e.state) ~
          ("steps" -> e.stepTokens) ~
          ("iteration" -> e.iteration)
      case None => socket.executionConfiguration match {
        case Some(ec: ExecutionConfiguration) => socket.executionStatistics match {
          case Some(es: ExecutionStatistics) =>
            ("mode" -> ec.executionMode.toString) ~
              ("state" -> es.terminationReason.toString) ~
              ("totalExecutionTime" -> es.totalExecutionTime.toString) ~
              ("computationTime" -> es.computationTime.toString)
          case None =>
            ("mode" -> ec.executionMode.toString())
        }
        case None =>
          ("state" -> "undetermined")
      }
    }
    ("provider" -> "state") ~ reply
  }
}

/**
 * DataProvider which serves execution, graph and system configurations.
 *
 * The execution configuration is taken from the WebSocketConsoleServer which
 * owns the execution mode object. The graph configuration is taken from the
 * coordinator. Finally, the system information is taken from the Java
 * System.getProperties collection.
 *
 * @constructor create a new ConfigurationDataProvider
 * @param socket the WebSocketConsoleServer (who knows the exec conf)
 * @param coordinator the Coordinator (who knows the graph conf)
 */
class ConfigurationDataProvider[Id](socket: WebSocketConsoleServer[Id],
                                    coordinator: Coordinator[Id, _])
    extends DataProvider {
  def fetch(): JObject = {
    val executionConfiguration = socket.executionConfiguration match {
      case Some(e: ExecutionConfiguration) => Toolkit.unpackObject(e)
      case otherwise                       => JString("unknown")
    }
    ("provider" -> "configuration") ~
      ("executionConfiguration" -> executionConfiguration) ~
      ("graphConfiguration" -> Toolkit.unpackObjects(Array(socket.graphConfiguration))) ~
      ("systemProperties" -> propertiesAsScalaMap(System.getProperties))
  }
}

/**
 * DataProvider which serves akka log messages from the coordinator.
 *
 * @constructor create a new LogDataProvider
 * @param coordinator the Coordinator
 */
class LogDataProvider[Id](coordinator: Coordinator[Id, _]) extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "log") ~
      ("messages" -> coordinator.getLogMessages)
  }
}

/** Data structure used to model client control requests */
case class ControlsRequest(
  control: Option[String])

/**
 * Provider that accepts execution commands.
 *
 * @constructor create a new ControlsProvider
 * @param socket the WebSocketConsoleServer
 * @param msg the request by the client
 */
class ControlsProvider(socket: WebSocketConsoleServer[_],
                       msg: JValue) extends DataProvider {

  implicit val formats = DefaultFormats
  var execution: Option[Execution] = socket.execution

  def command(e: Execution, command: String): JObject = {
    command match {
      case "step"      => e.step
      case "collect"   => e.collect
      case "pause"     => e.pause
      case "continue"  => e.continue
      case "reset"     => e.reset
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

/** Data structure used to model client break condition requests */
case class BreakConditionsRequest(
  action: Option[String],
  name: Option[String],
  id: Option[String],
  props: Option[Map[String, String]])

/** Data structure used to model replies to the client */
case class BreakConditionContainer(
  id: String,
  name: String,
  props: Map[String, String])

/**
 * Provider that can be used to add, remove and check on break conditions.
 *
 * The break conditions are stored with the interactive execution mode. Each
 * condition has a unique ID which it receives when it's being created. During
 * the computation, various break conditions will be checked at different
 * times.
 *
 * Whether the client supplies an action (to add or remove a condition) or not,
 * the provider will always answer with a list of configured conditions and a
 * list of reached conditions. If the action could not be performed (e.g. if
 * an invalid vertex ID has been supplied) an error message will be included.
 *
 * @constructor create a new BreakConditionsProvider
 * @param coordinator the Coordinator
 * @param socket the WebSocketConsoleServer
 * @param msg the request by the client
 */
class BreakConditionsProvider[Id](coordinator: Coordinator[Id, _],
                                  socket: WebSocketConsoleServer[Id],
                                  msg: JValue) extends DataProvider {

  implicit val formats = DefaultFormats
  var execution: Option[Execution] = socket.execution
  val workerApi = coordinator.getWorkerApi

  def fetchConditions(e: Execution): JObject = {
    val active = e.conditions.map {
      case (id, c) =>
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
                            // Create the condition. The input is validated
                            // inside the constructor of BreakCondition and
                            // when a requirement fails, an exception is thrown
                            val condition = new BreakCondition(socket.graphConfiguration,
                              c, n, props, workerApi)
                            e.addCondition(condition)
                            fetchConditions(e)
                          } catch {
                            case ex: IllegalArgumentException =>
                              fetchConditions(e) ~
                                ("error" -> ex.getMessage.toString)
                          }
                        case None => fetchInvalid(msg, "executionConfiguration unavailable!")
                      }
                    case None => fetchInvalid(msg, "missing props!")
                  }
                } catch {
                  case e: NoSuchElementException =>
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

/** Data structure used to model client graph data requests */
case class GraphDataRequest(
  vertexIds: Option[List[String]],
  vicinityRadius: Option[Int],
  vicinityIncoming: Option[Boolean],
  exposeVertices: Option[Boolean],
  query: Option[String],
  targetCount: Option[Int],
  topCriterium: Option[String],
  substring: Option[String],
  signalThreshold: Option[Double],
  collectThreshold: Option[Double])

/**
 * Provider that can be used to query the graph.
 *
 * @constructor create a new GraphDataProvider
 * @param coordinator the Coordinator
 * @param msg the request by the client
 */
class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue)
    extends DataProvider {

  implicit val formats = DefaultFormats

  val workerApi = coordinator.getWorkerApi
  var vertexIdStrings = List[String]()
  var targetCount = 5
  var vicinityRadius = 0
  var vicinityIncoming = false
  var exposeVertices = false
  var signalThreshold = 0.01
  var collectThreshold = 0.0

  /**
   * Recursively load the vicinity of a vertex.
   *
   * The vicinity of a vertex is the set of vertices that can be reached by
   * traveling a maximum of {{radius}} times along the edges leading away
   * from the vertex. A {{radius}} of 1 denotes only the vertices that share
   * an edge with the node. Because a vertex already knows its outgoing edges,
   * it is cheap to find the outgoing vicinity. However, for the incoming
   * vicinity, an aggregation has to be performed to find the vertices that
   * target one of the vertices we're interested in, hence it's an expensive
   * operation.
   *
   * @param sourceIds set of vertex IDs to use in the search
   * @param radius how far to travel from the source vertices
   * @param incoming also consider incoming edges (costs {{radius}} aggregations)
   * @return the original set plus the set of vertex IDs in the vicinity
   */
  def findVicinity(sourceIds: Set[Id], radius: Int = 3,
                   incoming: Boolean = false): Set[Id] = {
    if (radius == 0) { sourceIds }
    else {
      if (incoming) {
        val vicinityIds = workerApi.aggregateAll(
          new FindVertexVicinitiesByIdsAggregator[Id](sourceIds))
        sourceIds ++ findVicinity(vicinityIds, radius - 1, true)
      } else {
        sourceIds ++ findVicinity(sourceIds.map { id =>
          workerApi.forVertexWithId(id, { vertex: Inspectable[Id, _] =>
            vertex.getTargetIdsOfOutgoingEdges.map(_.asInstanceOf[Id]).toSet
          })
        }.flatten, radius - 1, false)
      }
    }
  }

  /**
   * Fetch a JObject representation of the vertices and edges of the graph.
   *
   * This function will load the vertices as supplied by the {{vertexIds}}
   * set but also any vertices in the object-scope variable
   * {{vertexIdStrings}}. The client will usually request a specific part of
   * the graph, but also supply a list of vertices that should be loaded.
   *
   * All the other fetch functions make use of this function to finalize
   * the request.
   *
   * @param vertexIds set of vertex IDs to load
   * @return the JObject containing the requested vertices and their edges
   */
  def fetchGraph(vertexIds: Set[Id] = Set[Id]()): JObject = {
    val vertices = workerApi.aggregateAll(
      new FindVerticesByIdsAggregator[Id](vertexIdStrings))
    val vicinityIds = findVicinity(vertexIds ++ vertices.map { _.id }.toSet,
      vicinityRadius, vicinityIncoming)
    val (lowestState, highestState, graph) =
      workerApi.aggregateAll(new GraphAggregator[Id](vicinityIds, exposeVertices))
    ("highestState" -> highestState) ~
      ("lowestState" -> lowestState) ~
      graph
  }

  /** Fetch vertices ordered by their highest or lowest state. */
  def fetchByTopState(inverted: Boolean = false): JObject = {
    val topState = workerApi.aggregateAll(
      new TopStateAggregator[Id](targetCount, inverted)).take(targetCount)
    val vertexIds = topState.foldLeft(Set[Id]()) { (acc, m) => acc + m._2 }
    fetchGraph(vertexIds)
  }

  /** Fetch vertices ordered by their degree. */
  def fetchByTopDegree(): JObject = {
    val vertexIds = workerApi.aggregateAll(new TopDegreeAggregator[Id](targetCount))
      .toSeq.sortBy(-_._2)
      .take(targetCount)
      .map { _._1 }
      .toSet
    fetchGraph(vertexIds)
  }

  /** Fetch vertices with a {{scoreType}} score above the threshold. */
  def fetchByAboveThreshold(scoreType: String): JObject = {
    val threshold = if (scoreType == "signal") signalThreshold else collectThreshold
    val aboveThreshold = workerApi.aggregateAll(
      new AboveThresholdAggregator[Id](targetCount, scoreType, threshold)).take(targetCount)
    val vertexIds = aboveThreshold.foldLeft(Set[Id]()) { (acc, m) => acc + m._2 }
    fetchGraph(vertexIds)
  }

  /** Fetch vertices whose IDs contain the given substring {{s}}. */
  def fetchBySubstring(s: String): JObject = {
    val vertexIds = workerApi.aggregateAll(
      new FindVertexIdsBySubstringAggregator[Id](s, targetCount))
    fetchGraph(vertexIds)
  }

  /** Fetch a random sample of vertices. */
  def fetchSample(): JObject = {
    val vertexIds = workerApi.aggregateAll(new SampleAggregator[Id](targetCount))
    fetchGraph(vertexIds)
  }

  def fetch(): JObject = {
    val request = (msg).extract[GraphDataRequest]

    // Override default values if any
    request.vertexIds match {
      case Some(ids) => vertexIdStrings = ids.take(1000)
      case otherwise =>
    }
    request.targetCount match {
      case Some(t)   => targetCount = List(t, 1000).min
      case otherwise =>
    }
    request.vicinityRadius match {
      case Some(r)   => vicinityRadius = List(r, 4).min
      case otherwise =>
    }
    request.vicinityIncoming match {
      case Some(b)   => vicinityIncoming = b
      case otherwise =>
    }
    request.exposeVertices match {
      case Some(b)   => exposeVertices = b
      case otherwise =>
    }
    request.signalThreshold match {
      case Some(t)   => signalThreshold = t
      case otherwise =>
    }
    request.collectThreshold match {
      case Some(t)   => collectThreshold = t
      case otherwise =>
    }

    // route request and fetch graph data
    val graphData = request.query match {
      case Some("substring") => request.substring match {
        case Some(s)   => fetchBySubstring(s)
        case otherwise => fetchInvalid(msg, "missing substring")
      }
      case Some("vertexIds") => request.vertexIds match {
        case Some(ids) => fetchGraph()
        case otherwise => fetchInvalid(msg, "missing vertexIds")
      }
      case Some("top") => request.topCriterium match {
        case Some("Highest state")         => fetchByTopState()
        case Some("Lowest state")          => fetchByTopState(true)
        case Some("Highest degree")        => fetchByTopDegree()
        case Some("Above signal thresh.")  => fetchByAboveThreshold("signal")
        case Some("Above collect thresh.") => fetchByAboveThreshold("collect")
        case otherwise                     => new InvalidDataProvider(msg, "invalid top criterium").fetch
      }
      case otherwise => fetchInvalid(msg, "missing query")
    }

    ("provider" -> "graph") ~
      graphData
  }
}

/**
 * Provider offering resource and other statistics on workers and nodes.
 *
 * @constructor create a new ReourcesDataProvider
 * @param coordinator the Coordinator
 * @param msg the request by the client
 */
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

