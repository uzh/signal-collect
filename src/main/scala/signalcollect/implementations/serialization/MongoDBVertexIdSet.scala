package signalcollect.implementations.serialization

import signalcollect.interfaces._
import com.mongodb.casbah.Imports._

class MongoDBVertexIdSet(vertexStore: Storage) extends VertexIdSet with DefaultSerializer {

  protected var toHandle = vertexSetFactory
  protected def vertexSetFactory = MongoConnection()("todo")(getRandomString("", 16))

  def +=(vertexId: Any): Unit = {
    toHandle += MongoDBObject("k" -> write(vertexId))
  }

  def -=(vertexId: Any): Unit = {
    toHandle.remove(MongoDBObject("k" -> write(vertexId)))
  }

  def isEmpty(): Boolean = {
    toHandle.isEmpty
  }

  def size(): Long = { toHandle.size }

  def foreach[U](f: (Vertex[_, _]) => U) = {
    toHandle.foreach{s => f(vertexStore.vertices.get(read((s.getAs[Array[Byte]]("k")).get))); toHandle.remove(s) 
    }
  }

  def foreachWithSnapshot[U](f: (Vertex[_, _]) => U) = {
	  val toHandleSnapshot = toHandle
      toHandle = vertexSetFactory
      toHandleSnapshot.foreach{s => f(vertexStore.vertices.get(read((s.getAs[Array[Byte]]("k")).get))); toHandle.remove(s)}
  }
  
  def getRandomString(prefix: String, length: Int): String = {
	  val chars = (('a' to 'z') ++ ('A' to 'Z') ++ ('1' to '9')).toList
	  var res = prefix
	  for(i <- 0 to length) {
	 	  res+=chars(scala.util.Random.nextInt(chars.size))
	  }
	  res
  }
}

trait MongoDBToDoList extends DefaultStorage {
	override protected def vertexSetFactory: VertexIdSet = new InMemoryVertexIdSet(this)
}