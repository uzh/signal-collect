package com.signalcollect.implementations.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    while (!(vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty) && isMailboxEmpty) {
      val completed = vertexStore.toCollect.foreach(
        (vertexId, uncollectedSignals) => {
          val collectExecuted = executeCollectOperationOfVertex(vertexId, uncollectedSignals, addToSignal = false)
          if (collectExecuted) {
            vertexStore.toSignal.add(vertexId)
          }
        }, removeAfterProcessing = true, breakCondition = () => !isMailboxEmpty)
      if (completed) {
        vertexStore.toSignal.applyToNext(executeSignalOperationOfVertex(_), removeAfterProcessing = true)
      }
    }
  }
}