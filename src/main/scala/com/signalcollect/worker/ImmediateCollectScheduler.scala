package com.signalcollect.worker

trait ImmediateCollectScheduler[Id, Signal] extends AkkaWorker[Id, Signal] {
  override def scheduleOperations {
    if (!vertexStore.toCollect.isEmpty) {
      vertexStore.toCollect.process(executeCollectOperationOfVertex(_))
    }
    if (maySignal && !vertexStore.toSignal.isEmpty) {
      val maxSignalingTime = 50000000 // 50 milliseconds
      val signalingStart = System.nanoTime
      do {
        vertexStore.toSignal.process(executeSignalOperationOfVertex(_), Some(100))
      } while (System.nanoTime - signalingStart < maxSignalingTime)
      messageBus.flush
      maySignal = false
    }
    if (!vertexStore.toSignal.isEmpty && !awaitingSignalPermission) {
      messageBus.sendToActor(self, MaySignal)
      awaitingSignalPermission = true
    }
  }
}