package com.signalcollect.console

import com.signalcollect.interfaces.Coordinator
import com.signalcollect.interfaces.Inspectable
import com.signalcollect.TopKFinder
import scala.reflect._
import scala.reflect.runtime.{universe => ru}
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import com.signalcollect.interfaces.WorkerStatus

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
  search: Option[String], 
  vicinity: Option[String],
  topk: Option[Int]
)

class GraphDataProvider[Id](coordinator: Coordinator[Id, _], msg: JValue) 
                            extends DataProvider {

  implicit val formats = DefaultFormats

  val workerApi = coordinator.getWorkerApi 

  def findVicinity(vertexIds: List[Id], depth: Int = 3): List[Id] = {
    if (depth == 0) { vertexIds }
    else {
      findVicinity(vertexIds.map { id =>
        workerApi.forVertexWithId(id, { vertex: Inspectable[Id,_] =>
          vertex.getTargetIdsOfOutgoingEdges.map(_.asInstanceOf[Id]).toList
        })
      }.flatten, depth - 1)
    }
  }

  def fetchVicinity(id: String): JObject = {
    val vertex = workerApi.aggregateAll(
                 new FindVertexByIdAggregator[Id](id))
    val vicinity = vertex match {
      case Some(v) => 
        findVicinity(List(v.id))
      case None => List[Id]()
    }
    workerApi.aggregateAll(new GraphAggregator[Id](vicinity))
  }

  def fetchTopk(n: Int): JObject = {
    val topk = new TopKFinder[Int](n)
    val nodes = workerApi.aggregateAll(topk)
    workerApi.aggregateAll(new GraphAggregator(nodes.toList.map(_._1)))
  }

  def fetchAll(): JObject = {
    workerApi.aggregateAll(new GraphAggregator)
  }

  def fetch(): JObject = {
    val request = (msg).extract[GraphDataRequest]
    val graphData = request.search match {
      case Some("vicinity") => request.vicinity match {
        case Some(id) => fetchVicinity(id)
        case otherwise => fetchInvalid(msg)
      }
      case Some("topk") => request.topk match {
        case Some(n) => fetchTopk(n)
        case otherwise => new InvalidDataProvider(compact(render(msg))).fetch
      }
      case otherwise => fetchAll
    }
    
    ("provider" -> "graph") ~
    graphData
  }
}

class ResourcesDataProvider(coordinator: Coordinator[_, _], msg: JValue)
      extends DataProvider {

  def unpackObjectList[T: ClassTag: ru.TypeTag](obj: Array[T]): List[JField] = {
    val methods = ru.typeOf[T].members.filter { m =>
      m.isMethod && m.asMethod.isStable 
    }
    methods.map { m =>
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
        }
      }
      JField(m.name.toString, values)
    }.toList
  }

  def fetch(): JObject = {
    val inboxSize: Long = coordinator.getGlobalInboxSize

    val ws: Array[WorkerStatus] = 
      (coordinator.getWorkerStatus)
    val wstats = unpackObjectList(ws.map(_.workerStatistics))
    val sstats = unpackObjectList(ws.map(_.systemInformation))

    ("provider" -> "resources") ~
    ("timestamp" -> System.currentTimeMillis) ~
    ("inboxSize" -> inboxSize) ~
    ("workerStatistics" -> JObject(wstats) ~ JObject(sstats))
  }
}

