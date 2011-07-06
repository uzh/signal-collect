package signalcollect.implementations.logging

import signalcollect.interfaces._
import akka.actor.Actor
import akka.actor.ActorRef
import signalcollect.util.Constants._

class ActorLoggerAdapter(coordAddr: String) extends Logger {
  
  val actorRef = Actor.remote.actorFor(LOGGER_SERVICE_NAME, coordAddr, LOGGER_SERVICE_PORT)
  
  def receive(loggingMessage: Any) = actorRef ! loggingMessage

}