package signalcollect.interfaces

import scala.annotation.elidable

trait Logger extends MessageRecipient[Any] {
  
  @elidable(scala.annotation.elidable.ALL)
  def debug(msg: Any) = logMessage("debug", msg)

  @elidable(scala.annotation.elidable.CONFIG)
  def config(msg: Any) = logMessage("config", msg)

  @elidable(scala.annotation.elidable.INFO)
  def info(msg: Any) = logMessage("info", msg)

  @elidable(scala.annotation.elidable.SEVERE)
  def severe(msg: Any) = logMessage("severe", msg)

  def logMessage(level: String, message: Any) {
    if (level equals "severe")
      System.err.println(level.toUpperCase + " " + message)
    else
      println(level.toUpperCase + " " + message)
  }
}