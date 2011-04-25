package signalcollect.implementations.serialization

import signalcollect.interfaces._
import com.mongodb.casbah.Imports._

class MongoDBStoreAllOnDisk(messageBus: MessageBus[Any, Any]) extends MongoDBStore(messageBus) {

  override def vertexSetFactory = null;

  protected var toSignalCollection = MongoConnection()("sig")(randomID)
  protected var toCollectCollection = MongoConnection()("col")(randomID)

  override def addForSignling(vertexId: Any) = {
    toSignalCollection += MongoDBObject("k" -> write(vertexId))
  }
  override def addForCollecting(vertexId: Any) = {
    toCollectCollection += MongoDBObject("k" -> write(vertexId))
  }

  override def removeFromSignaling(vertexId: Any) = {
    toSignalCollection.remove(MongoDBObject("k" -> write(vertexId)))
  }

  override def removeFromCollecting(vertexId: Any) = {
    toCollectCollection.remove(MongoDBObject("k" -> write(vertexId)))
  }
  override def numberOfVerticesToSignal = toSignalCollection.size
  override def numberOfVerticesToCollect = toCollectCollection.size

  override def hasToSignal = !toSignalCollection.isEmpty
  override def hasToCollect = !toCollectCollection.isEmpty

  override def foreachToSignal[U](f: (Vertex[_, _]) => U) = {
    toSignalCollection.foreach { s => f(getVertexWithID(read((s.getAs[Array[Byte]]("k")).get))); toSignalCollection.remove(s) }
  }
  override def foreachToCollect[U](f: (Vertex[_, _]) => U, makeSnapShot: Boolean) = {
    toCollectCollection.foreach { c => f(getVertexWithID(read((c.getAs[Array[Byte]]("k")).get))); toCollectCollection.remove(c) }
  }
}