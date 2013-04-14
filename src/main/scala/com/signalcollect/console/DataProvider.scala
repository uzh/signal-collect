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
import com.signalcollect.interfaces.WorkerStatus
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

class ControlsProvider[Id](socket: WebSocketConsoleServer[Id],
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
    val request = (msg).extract[ControlsRequest]
    val reply = execution match {
      case Some(e) => request.control match {
        case Some(action) => action match {
          case "step" => computationStep(e)
          case "pause" => computationPause(e)
          case "continue" => computationContinue(e)
          case "reset" => computationReset(e)
          case "terminate" => computationTerminate(e)
          case otherwise => fetchInvalid(msg, "invalid control!")
        }
        case None => fetchInvalid(msg, "missing control!")
      }
      case None => fetchInvalid(msg, "interactive execution is unavailable!")
    }
    ("provider" -> "controls") ~ reply
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
  vicinityRadius: Option[Int],
  query: Option[String], 
  id: Option[String],
  maxVertices: Option[Int],
  topCriterium: Option[String]
)

class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue) 
                            extends DataProvider {

  implicit val formats = DefaultFormats

  val workerApi = coordinator.getWorkerApi 

  def findVicinity(vertexIds: List[Id], radius: Int = 3): List[Id] = {
    if (radius == 0) { vertexIds }
    else {
      findVicinity(vertexIds.map { id =>
        workerApi.forVertexWithId(id, { vertex: Inspectable[Id,_] =>
          vertex.getTargetIdsOfOutgoingEdges.map(_.asInstanceOf[Id]).toList
        })
      }.flatten, radius - 1)
    }
  }

  def fetchId(id: String, radius: Int): JObject = {
    val result = workerApi.aggregateAll(
                 new FindVertexByIdAggregator[Id](id))
    val (vertex, vicinity) = result match {
      case Some(v) => (List[Id](v.id), findVicinity(List(v.id), radius))
      case None => (List[Id](), List[Id]())
    }
    workerApi.aggregateAll(new GraphAggregator[Id](vertex, vicinity))
  }

  def fetchTopStates(n: Int, radius: Int): JObject = {
    val topState = workerApi.aggregateAll(new TopStateAggregator[Id]())
    val nodes = topState.foldLeft(List[Id]()){ (acc, m) => acc ++ m._2 }.take(n)
    val vicinity = findVicinity(nodes, radius)
    workerApi.aggregateAll(new GraphAggregator(nodes, vicinity))
  }

  def fetchTopDegree(n: Int, radius: Int, direction: String): JObject = {
    val topDegree = workerApi.aggregateAll(new TopDegreeAggregator[Id]())
    val nodes = topDegree.foldLeft(List[Id]()){ (acc, m) => acc ++ m._2 }.take(n)
    val vicinity = findVicinity(nodes, radius)
    workerApi.aggregateAll(new GraphAggregator[Id](nodes, vicinity))
  }

  def fetchSample(n: Int, radius: Int): JObject = {
    val ids = workerApi.aggregateAll(new SampleVertexIds(n))
    val nodes = ids.foldLeft(List[Id]()){ (acc, id) =>
      workerApi.aggregateAll(new FindVertexByIdAggregator[Id](id.toString)) match {
        case Some(v) => v.id :: acc
        case None    => acc
      }
    }
    workerApi.aggregateAll(new GraphAggregator[Id](nodes, findVicinity(nodes, radius)))
  }

  def fetch(): JObject = {
    val request = (msg).extract[GraphDataRequest]
    val m = request.maxVertices match {
      case Some(maxVertices) => maxVertices
      case otherwise => 100
    }
    val r = request.vicinityRadius match {
      case Some(radius) => radius
      case otherwise => 0
    }
    val graphData = request.query match {
      case Some("id") => request.id match {
        case Some(id) => fetchId(id, r)
        case otherwise => fetchInvalid(msg, "missing id")
      }
      case Some("top") => request.topCriterium match {
        case Some("State (Numerical)") => fetchTopStates(m, r)
        case Some("Degree (Both)") => fetchTopDegree(m, r, "both")
        case otherwise => new InvalidDataProvider(msg).fetch
      }
      case otherwise => fetchSample(m, r)
    }
    
    ("provider" -> "graph") ~
    graphData
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _], msg: JValue)
      extends DataProvider {


  def fetch(): JObject = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: Array[WorkerStatus] = 
      (coordinator.getWorkerStatus)
    val wstats = Toolkit.unpackObjects(ws.map(_.workerStatistics))
    val sstats = Toolkit.unpackObjects(ws.map(_.systemInformation))

    ("provider" -> "resources") ~
    ("timestamp" -> System.currentTimeMillis) ~
    ("inboxSize" -> inboxSize) ~
    ("workerStatistics" -> wstats ~ sstats)
  }
}

