/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.signalcollect.util

import java.lang.management.ManagementFactory
import scala.collection.JavaConversions._

case class Stats(
  description: String,
  nanosecondsPerExecution: Long)

object Benchmark {
  def apply(f: () => Unit, description: String = "", warmups: Int = 1000, measuredRepetitions: Int = 10000): Stats = {
    if (description != "") {
      println(s"Warming up $description.")
    }
    var i = 0
    while (i < warmups) {
      f()
      i += 1
    }
    i = 0
    sleepUntilGcInactiveForXSeconds(3)
    if (description != "") {
      println(s"Benchmarking $description.")
    }
    val startTime = System.nanoTime
    while (i < measuredRepetitions) {
      f()
      i += 1
    }
    val finishTime = System.nanoTime
    val totalNanoTime = finishTime - startTime
    val nanosecondsPerExecution = totalNanoTime.toDouble / measuredRepetitions.toDouble
    println(s"$description took $nanosecondsPerExecution nanoseconds per execution")
    Stats(description,
      nanosecondsPerExecution.toLong)
  }

  def sleepUntilGcInactiveForXSeconds(x: Int, maxSeconds: Int = 600) {
    def getTime = System.currentTimeMillis / 1000
    val startTime = getTime
    val gcs = ManagementFactory.getGarbageCollectorMXBeans
    val sunGcs = gcs.map(_.asInstanceOf[com.sun.management.GarbageCollectorMXBean])
    def collectionTime = sunGcs.map(_.getCollectionTime).sum
    def collectionDelta(oldGcTime: Long) = collectionTime - oldGcTime
    def secondsPassed = getTime - startTime
    var secondsWithoutGc = 0
    var lastGcTime = collectionTime
    while (secondsWithoutGc < x && secondsPassed < maxSeconds) {
      Thread.sleep(1000)
      val delta = collectionDelta(lastGcTime)
      if (delta > 0) {
        secondsWithoutGc = 0
        lastGcTime = collectionTime
      } else {
        secondsWithoutGc += 1
      }
    }
  }

}
