package com.signalcollect.implementations.storage

import java.util.Set
import com.signalcollect.interfaces._
import com.signalcollect.implementations.serialization._
import java.util.Set


 class DefaultStorage(val messageBus: MessageBus[Any]) extends Storage(messageBus) with DefaultSerializer {
  var vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore = new InMemoryStorage(this)
  var toCollect = vertexSetFactory //holds all vertex ids that need to signal
  var toSignal = vertexSetFactory //holds all vertex ids that need to collect
  protected def vertexSetFactory: VertexIdSet = new InMemoryVertexIdSet(this)
  def cleanUp {
    vertexStoreFactory.cleanUp
    toCollect.cleanUp
    toSignal.cleanUp
  }
}