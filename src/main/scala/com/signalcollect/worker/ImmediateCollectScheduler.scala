package com.signalcollect.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    while (!messageQueue.hasMessages && !(vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty)) {
      var collectingDone = true
      if (!vertexStore.toCollect.isEmpty) {
        val collectIter = vertexStore.toCollect.iterator
        collectingDone = !collectIter.hasNext
        while (!messageQueue.hasMessages && !collectingDone) {
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
        while (!messageQueue.hasMessages && signalIter.hasNext) {
          val vertex = signalIter.next
          signalIter.remove
          val signalExecuted = executeSignalOperationOfVertex(vertex)
        }
        messageBus.flush
      }
    }
  }
}