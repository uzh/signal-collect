package com.signalcollect.nodeprovisioning.torque

/**
 * Determines the priority in torque's scheduling queue
 */
object TorquePriority {
  val superfast = "#PBS -l walltime=00:59:59,mem=50gb"
  val fast = "#PBS -l walltime=11:59:59,mem=50gb"
  val slow = "#PBS -l walltime=200:59:59,mem=50gb"
}