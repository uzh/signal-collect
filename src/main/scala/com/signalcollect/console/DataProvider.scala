package com.signalcollect.console

import com.signalcollect.interfaces.Coordinator
import com.signalcollect.ExecutionConfiguration
import com.signalcollect.interfaces.Inspectable
import com.signalcollect.TopKFinder
import com.signalcollect.SampleVertexIds
import scala.reflect._
import scala.reflect.runtime.{universe => ru}
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.interfaces.WorkerStatus

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
}

trait DataProvider {
  def fetch(): JObject
  def fetchInvalid(msg: JValue = JString("")): JObject = {
    new InvalidDataProvider(compact(render(msg))).fetch
  }
}

class InvalidDataProvider(msg: String) extends DataProvider {
  def fetch(): JObject = {
    ("provider" -> "invalid") ~
    ("msg" -> ("Received an invalid message: " + msg))
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
      case Some(e: ExecutionConfiguration) => Toolkit.unpackObject(Array(e))
      case otherwise => JObject(List(JField("unknown", "unknown")))
    }
    ("provider" -> "executionConfiguration") ~ 
    ("executionConfiguration" -> executionConfiguration)
  }
}

case class ApiRequest(
  provider: String, 
  control: Option[String]
)

class ApiProvider[Id](socket: WebSocketConsoleServer[Id],
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
    val request = (msg).extract[ApiRequest]
    val reply = execution match {
      case Some(e) => request.control match {
        case Some(action) => action match {
          case "step" => computationStep(e)
          case "pause" => computationPause(e)
          case "continue" => computationContinue(e)
          case "reset" => computationReset(e)
          case "terminate" => computationTerminate(e)
          case otherwise => fetchInvalid(msg)
        }
        case None => fetchInvalid(msg)
      }
      case None => fetchInvalid(msg)
    }
    ("provider" -> "controls") ~ reply
  }
}


case class GraphDataRequest(
  provider: String, 
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
        case otherwise => fetchInvalid(msg)
      }
      case Some("top") => request.topCriterium match {
        case Some("State (Numerical)") => fetchTopStates(m, r)
        case Some("Degree (Both)") => fetchTopDegree(m, r, "both")
        case otherwise => new InvalidDataProvider(compact(render(msg))).fetch
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
    val wstats = Toolkit.unpackObject(ws.map(_.workerStatistics))
    val sstats = Toolkit.unpackObject(ws.map(_.systemInformation))

    ("provider" -> "resources") ~
    ("timestamp" -> System.currentTimeMillis) ~
    ("inboxSize" -> inboxSize) ~
    ("workerStatistics" -> wstats ~ sstats)
  }
}

