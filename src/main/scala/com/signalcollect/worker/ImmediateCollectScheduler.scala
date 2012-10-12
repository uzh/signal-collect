package com.signalcollect.worker

trait ImmediateCollectScheduler extends AkkaWorker {
  override def scheduleOperations {
    var collectingDone = true
    if (!vertexStore.toCollect.isEmpty) {
      val collectIter = vertexStore.toCollect.iterator
      collectingDone = !collectIter.hasNext
      while (!collectingDone) {
        val vertex = collectIter.next
        collectIter.remove
        val collectExecuted = executeCollectOperationOfVertex(vertex, addToSignal = false)
        if (collectExecuted) {
          executeSignalOperationOfVertex(vertex)
        }
        collectingDone = !collectIter.hasNext
      }
    }
    if (maySignal && !vertexStore.toSignal.isEmpty) {
      var signalsLeft = 1000
      val signalIter = vertexStore.toSignal.iterator
      while (signalsLeft > 0 && signalIter.hasNext) {
        val vertex = signalIter.next
        signalIter.remove
        val signalExecuted = executeSignalOperationOfVertex(vertex)
        signalsLeft -= 1
      }
      messageBus.flush
    }
    if (!vertexStore.toSignal.isEmpty) {
      messageBus.sendToActor(self, MaySignal)
    }
  }
}

//trait ImmediateCollectScheduler extends AkkaWorker {
//  override def scheduleOperations {
//    while (!messageQueue.hasMessages && !(vertexStore.toSignal.isEmpty && vertexStore.toCollect.isEmpty)) {
//      var collectingDone = true
//      if (!vertexStore.toCollect.isEmpty) {
//        val collectIter = vertexStore.toCollect.iterator
//        collectingDone = !collectIter.hasNext
//        while (!messageQueue.hasMessages && !collectingDone) {
//          val vertex = collectIter.next
//          collectIter.remove
//          val collectExecuted = executeCollectOperationOfVertex(vertex, addToSignal = false)
//          if (collectExecuted) {
//            executeSignalOperationOfVertex(vertex)
//          }
//          collectingDone = !collectIter.hasNext
//        }
//      }
//      if (collectingDone && !vertexStore.toSignal.isEmpty) {
//        val signalIter = vertexStore.toSignal.iterator
//        while (!messageQueue.hasMessages && signalIter.hasNext) {
//          val vertex = signalIter.next
//          signalIter.remove
//          val signalExecuted = executeSignalOperationOfVertex(vertex)
//        }
//        messageBus.flush
//      }
//    }
//  }
//}