package signalcollect.interfaces

sealed trait LogMessage
case class Other(msg: Any) extends LogMessage
case class Config(msg: Any) extends LogMessage
case class Info(msg: Any) extends LogMessage
case class Severe(msg: Any) extends LogMessage
case class Debug(msg: Any) extends LogMessage