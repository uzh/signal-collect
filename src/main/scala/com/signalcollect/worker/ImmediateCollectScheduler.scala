package com.signalcollect.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    vertexStore.toCollect.process(executeCollectOperationOfVertex(_))
    if (maySignal && !vertexStore.toSignal.isEmpty) {
      vertexStore.toSignal.process(executeSignalOperationOfVertex(_), Some(10000))
      messageBus.flush
      maySignal = false
    }
    if (!vertexStore.toSignal.isEmpty && !awaitingSignalPermission) {
      messageBus.sendToActor(self, MaySignal)
      awaitingSignalPermission = true
    }
  }
}