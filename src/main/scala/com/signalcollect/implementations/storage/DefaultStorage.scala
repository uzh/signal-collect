package com.signalcollect.implementations.storage

import java.util.Set
import com.signalcollect.interfaces._
import com.signalcollect.implementations.serialization._
import java.util.Set

/**
 * Default configuration for storing vertices and the toSignal and toCollect collections
 */
class DefaultStorage extends Storage with DefaultSerializer {
  var vertices = vertexStoreFactory
  protected def vertexStoreFactory: VertexStore = new InMemoryStorage(this)
  var toCollect = new DefaultVertexSignalBuffer //holds all signals that are not collected yet
  var toSignal = vertexSetFactory //holds all vertex ids that need to signal
  protected def vertexSetFactory: VertexIdSet = new InMemoryVertexIdSet(this)
  def cleanUp {
    vertexStoreFactory.cleanUp
    toCollect.cleanUp
    toSignal.cleanUp
  }
}