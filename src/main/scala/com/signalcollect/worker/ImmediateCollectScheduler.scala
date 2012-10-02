package com.signalcollect.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    while (!(vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty) && !messageQueue.hasMessages) {
      val completed = vertexStore.toCollect.foreach(
        (vertexId, uncollectedSignals) => {
          val collectExecuted = executeCollectOperationOfVertex(vertexId, uncollectedSignals, addToSignal = false)
          if (collectExecuted) {
            vertexStore.toSignal.add(vertexId)
          }
        }, removeAfterProcessing = true, breakCondition = () => messageQueue.hasMessages)
      if (completed) {
        vertexStore.toSignal.foreach(executeSignalOperationOfVertex(_), removeAfterProcessing = true)
      }
    }
  }
}