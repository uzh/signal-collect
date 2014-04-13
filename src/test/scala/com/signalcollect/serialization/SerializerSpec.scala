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
import com.signalcollect.GraphBuilder
import com.signalcollect.configuration.ActorSystemRegistry
import akka.serialization.SerializationExtension
import com.romix.akka.serialization.kryo.KryoSerializer

@RunWith(classOf[JUnitRunner])
class SerializerSpec extends SpecificationWithJUnit with Mockito {

  sequential

  "Kryo" should {

    "correctly serialize Scala immutable maps" in {
      val g = GraphBuilder.build
      try {
        // Scala uses special representations for small maps.
        val akka = ActorSystemRegistry.retrieve("SignalCollect").get
        val serialization = SerializationExtension(akka)
        val m1 = Some(Map(1 -> 1.5))
        val m2 = Some(Map(1 -> 1.5, 2 -> 5.4))
        val m3 = Some(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5))
        val m4 = Some(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2))
        val m5 = Some(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2, 6 -> 3.2))
        val s1 = serialization.findSerializerFor(m1)
        val s2 = serialization.findSerializerFor(m2)
        val s3 = serialization.findSerializerFor(m3)
        val s4 = serialization.findSerializerFor(m4)
        val s5 = serialization.findSerializerFor(m5)
        assert(s5.isInstanceOf[KryoSerializer])
        val bytes1 = s1.toBinary(m1)
        val bytes2 = s2.toBinary(m2)
        val bytes3 = s3.toBinary(m3)
        val bytes4 = s4.toBinary(m4)
        val bytes5 = s5.toBinary(m5)
        val b1 = s1.fromBinary(bytes1, manifest = None)
        val b2 = s2.fromBinary(bytes2, manifest = None)
        val b3 = s3.fromBinary(bytes3, manifest = None)
        val b4 = s4.fromBinary(bytes4, manifest = None)
        val b5 = s5.fromBinary(bytes5, manifest = None)
        assert(b1 == m1)
        assert(b2 == m2)
        assert(b3 == m3)
        assert(b4 == m4)
        assert(b5 == m5)
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala None" in {
      val g = GraphBuilder.build
      try {
        // Scala uses special representations for small maps.
        val akka = ActorSystemRegistry.retrieve("SignalCollect").get
        val serialization = SerializationExtension(akka)
        val none = None
        val s = serialization.findSerializerFor(none)
        assert(s.isInstanceOf[KryoSerializer])
        val bytes = s.toBinary(none)
        val b = s.fromBinary(bytes, manifest = None)
        assert(b == none)
        true
      } finally {
        g.shutdown
      }
    }

  }

  // TODO: None test

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