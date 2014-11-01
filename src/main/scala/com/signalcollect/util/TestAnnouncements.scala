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
 *  
 */

package com.signalcollect.util

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date

trait TestAnnouncements extends BeforeAndAfterAll {
  this: Suite =>

  var startTime: Long = System.currentTimeMillis

  println(s"Starting test setup for ${this.getClass.getSimpleName} @ $currentTime.")

  def timeSinceStart: Option[String] = {
    val delta = ((System.currentTimeMillis - startTime) / 1000).toInt
    val seconds = delta % 60
    val minutes = (delta / 60).floor.toInt
    if (minutes > 0) {
      Some(s"$minutes minutes and $seconds seconds")
    } else if (seconds > 0) {
      Some(s"$seconds seconds")
    } else {
      None
    }
  }

  def currentTime: String = {
    val now = Calendar.getInstance.getTime
    val formatter = new SimpleDateFormat("HH:mm")
    val timeString = formatter.format(now)
    timeString
  }

  override def beforeAll {
    val totalTime = timeSinceStart
    if (totalTime.isDefined) {
      println(s"Setup took ${timeSinceStart.get}, starting test execution.")
    }
  }

  override def afterAll {
    val totalTime = timeSinceStart
    if (totalTime.isDefined) {
      println(s"Finished tests in ${this.getClass.getSimpleName} @ $currentTime, ran for a total of ${totalTime.get}.")
    } else {
      println(s"Finished tests in ${this.getClass.getSimpleName} @ $currentTime, ran for less than a second.")
    }
  }

}
