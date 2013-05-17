/*
 *  @author Philip Stutz
 *
 *  Copyright 2011 University of Zurich
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

package com.signalcollect.serialization

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SerializerSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "DefaultSerializer" should {

    "correctly serialize/deserialize a Double" in {
      DefaultSerializer.read[Double](DefaultSerializer.write(1024.0)) === 1024.0
    }

    "correctly serialize/deserialize a job configuration" in {
      val job = new Job(
        100,
        Some(SpreadsheetConfiguration("some.emailAddress@gmail.com", "somePasswordHere", "someSpreadsheetNameHere", "someWorksheetNameHere")),
        "someUsername",
        "someJobDescription")
      DefaultSerializer.read[Job](DefaultSerializer.write(job)) === job
    }

  }

}

case class SpreadsheetConfiguration(
  gmailAccount: String,
  gmailPassword: String,
  spreadsheetName: String,
  worksheetName: String)

case class Job(
  var jobId: Int,
  var spreadsheetConfiguration: Option[SpreadsheetConfiguration],
  var submittedByUser: String,
  var jobDescription: String)