package signalcollect.implementations.storage

import java.util.Set
import signalcollect.interfaces._
import java.util.Set

 class DefaultStorage(val messageBus: MessageBus[Any, Any]) extends Storage(messageBus) with DefaultSerializer {
  var vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore = new InMemoryStorage(this)
  var toCollect = vertexSetFactory //holds all vertex ids that need to signal
  var toSignal = vertexSetFactory //holds all vertex ids that need to collect
  protected def vertexSetFactory: VertexIdSet = new InMemoryVertexIdSet(this)
}