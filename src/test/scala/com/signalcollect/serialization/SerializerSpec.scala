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
        kryoSerializeAndDeserialize(Map.empty[Int, Double])
        kryoSerializeAndDeserialize(Map(1 -> 1.5))
        kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4))
        kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5))
        kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2))
        kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2, 6 -> 3.2))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala immutable sets" in {
      val g = GraphBuilder.build
      try {
        // Scala uses special representations for small sets.
        kryoSerializeAndDeserialize(Set.empty[Int])
        kryoSerializeAndDeserialize(Set(1))
        kryoSerializeAndDeserialize(Set(1, 2))
        kryoSerializeAndDeserialize(Set(1, 2, 3))
        kryoSerializeAndDeserialize(Set(1, 2, 3, 4))
        kryoSerializeAndDeserialize(Set(1, 2, 3, 4, 5))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala None" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(None)
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala List" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(List.empty[Int])
        kryoSerializeAndDeserialize(List(1))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala Vector" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Vector.empty[Int])
        kryoSerializeAndDeserialize(Vector(1))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala Seq" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Seq.empty[Int])
        kryoSerializeAndDeserialize(Seq(1))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Scala Array" in {
      val g = GraphBuilder.build
      try {
        assert(kryoSerializeAndDeserializeSpecial(Array.empty[Int]).toList == List())
        assert(kryoSerializeAndDeserializeSpecial(Array(1)).toList == List(1))
        assert(kryoSerializeAndDeserializeSpecial(Array(1.0)).toList == List(1.0))
        assert(kryoSerializeAndDeserializeSpecial(Array(1l)).toList == List(1l))
        assert(kryoSerializeAndDeserializeSpecial(Array("abc")).toList == List("abc"))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Array[Array[Int]]" in {
      val g = GraphBuilder.build
      try {
        assert(kryoSerializeAndDeserializeSpecial(
          Array(Array(1, 2, 3), Array(3, 4, 5))).map(_.toList).toList == List(List(1, 2, 3), List(3, 4, 5)))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize integers" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Integer.valueOf(1))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize longs" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Long.box(1l))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize floats" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Float.box(1.0f))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize doubles" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Double.box(1.0d))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize booleans" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Boolean.box(true))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize shorts" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize(Short.box(1))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize strings" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize("abc")
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Java strings" in {
      val g = GraphBuilder.build
      try {
        val javaString: java.lang.String = "abc"
        kryoSerializeAndDeserialize(javaString)
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Tuple2" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize((1, "second"))
        true
      } finally {
        g.shutdown
      }
    }

    "correctly serialize Tuple3" in {
      val g = GraphBuilder.build
      try {
        kryoSerializeAndDeserialize((1, "second", 3.0))
        true
      } finally {
        g.shutdown
      }
    }

    def kryoSerializeAndDeserialize(instance: AnyRef) {
      val akka = ActorSystemRegistry.retrieve("SignalCollect").get
      val serialization = SerializationExtension(akka)
      val s = serialization.findSerializerFor(instance)
      //      println(s"${s.getClass} for ${instance.getClass}")
      assert(s.isInstanceOf[KryoSerializer])
      val bytes = s.toBinary(instance)
      val b = s.fromBinary(bytes, manifest = None)
      assert(b == instance)
    }

    def kryoSerializeAndDeserializeSpecial[T <: AnyRef](instance: T): T = {
      val akka = ActorSystemRegistry.retrieve("SignalCollect").get
      val serialization = SerializationExtension(akka)
      val s = serialization.findSerializerFor(instance)
      assert(s.isInstanceOf[KryoSerializer])
      val bytes = s.toBinary(instance)
      val b = s.fromBinary(bytes, manifest = None).asInstanceOf[T]
      b
    }

  }

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