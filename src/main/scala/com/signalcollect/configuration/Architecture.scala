package com.signalcollect.configuration

/**
 * Defines if the execution should take place locally or distributedly
 */
sealed trait Architecture extends Serializable

case class LocalArchitecture extends Architecture {
  override def toString = "LocalArchitecture"
}
case class DistributedArchitecture extends Architecture {
  override def toString = "DistributedArchitecture"
}