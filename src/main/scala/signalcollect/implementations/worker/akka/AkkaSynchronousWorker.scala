package signalcollect.implementations.worker.akka

import java.util._

import akka.actor.ReceiveTimeout
import signalcollect.api.Factory._
import signalcollect.interfaces._

//class AkkaSynchronousWorker(
//  workerId: Int,
//  mb: MessageBus[Any, Any],
//  mif: QueueFactory,
//  sf: StorageFactory) extends AkkaWorker(workerId, mb, mif, sf) {
//
//  self.receiveTimeout = Some(100L)
//
//  def receive = {
//
//    // after no messages in the inbox for self.receiveTimeout, ReceiveTimeout gets sent
//    case ReceiveTimeout =>
//      // handleIdling
//      if (isConverged || isPaused) { // if I have nothing to compute and the mailbox is empty, i'll be idle
//        if (mailboxIsEmpty)
//          setIdle(true)
//      }
//
//    // process messages
//    case x =>
//      setIdle(false)
//      process(x)
//
//  }
//
//}