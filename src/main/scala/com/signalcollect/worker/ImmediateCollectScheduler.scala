package com.signalcollect.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    while (!(vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty) && !messageQueue.hasMessages) {
      var collectingDone = true
      if (!vertexStore.toCollect.isEmpty) {
        val collectIter = vertexStore.toCollect.iterator
        collectingDone = !collectIter.hasNext
        while (!collectingDone && !messageQueue.hasMessages) {
          val vertex = collectIter.next
          collectIter.remove
          val collectExecuted = executeCollectOperationOfVertex(vertex, addToSignal = false)
          if (collectExecuted) {
            executeSignalOperationOfVertex(vertex)
          }
          collectingDone = !collectIter.hasNext
        }
      }
      if (collectingDone && !vertexStore.toSignal.isEmpty) {
        val signalIter = vertexStore.toSignal.iterator
        while (signalIter.hasNext && !messageQueue.hasMessages) {
          val vertex = signalIter.next
          signalIter.remove
          val signalExecuted = executeSignalOperationOfVertex(vertex)
        }
      }
    }
  }
}