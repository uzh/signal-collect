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

import com.signalcollect.{TestConfig, GraphBuilder}
import com.signalcollect.configuration.ActorSystemRegistry
import akka.serialization.SerializationExtension
import com.romix.akka.serialization.kryo.KryoSerializer
import org.scalatest.Matchers
import com.signalcollect.util.TestAnnouncements
import org.scalatest.FlatSpec

class SerializerSpec extends FlatSpec with Matchers with TestAnnouncements {

  "Kryo" should "correctly serialize Scala immutable maps" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      // Scala uses special representations for small maps.
      kryoSerializeAndDeserialize(Map.empty[Int, Double])
      kryoSerializeAndDeserialize(Map(1 -> 1.5))
      kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4))
      kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5))
      kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2))
      kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2, 6 -> 3.2))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala immutable sets" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      // Scala uses special representations for small sets.
      kryoSerializeAndDeserialize(Set.empty[Int])
      kryoSerializeAndDeserialize(Set(1))
      kryoSerializeAndDeserialize(Set(1, 2))
      kryoSerializeAndDeserialize(Set(1, 2, 3))
      kryoSerializeAndDeserialize(Set(1, 2, 3, 4))
      kryoSerializeAndDeserialize(Set(1, 2, 3, 4, 5))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala None" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(None)
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala List" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(List.empty[Int])
      kryoSerializeAndDeserialize(List(1))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala Vector" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Vector.empty[Int])
      kryoSerializeAndDeserialize(Vector(1))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala Seq" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Seq.empty[Int])
      kryoSerializeAndDeserialize(Seq(1))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Scala Array" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      assert(kryoSerializeAndDeserializeSpecial(Array.empty[Int]).toList == List())
      assert(kryoSerializeAndDeserializeSpecial(Array(1)).toList == List(1))
      assert(kryoSerializeAndDeserializeSpecial(Array(1.0)).toList == List(1.0))
      assert(kryoSerializeAndDeserializeSpecial(Array(1l)).toList == List(1l))
      assert(kryoSerializeAndDeserializeSpecial(Array("abc")).toList == List("abc"))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Array[Array[Int]]" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      assert(kryoSerializeAndDeserializeSpecial(
        Array(Array(1, 2, 3), Array(3, 4, 5))).map(_.toList).toList == List(List(1, 2, 3), List(3, 4, 5)))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize integers" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Integer.valueOf(1))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize longs" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Long.box(1l))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize floats" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Float.box(1.0f))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize doubles" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Double.box(1.0d))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize booleans" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Boolean.box(true))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize shorts" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize(Short.box(1))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize strings" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize("abc")
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Java strings" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      val javaString: java.lang.String = "abc"
      kryoSerializeAndDeserialize(javaString)
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Tuple2" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize((1, "second"))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  it should "correctly serialize Tuple3" in {
    val system = TestConfig.actorSystem(name = "SignalCollect", port = 2556)
    val g = GraphBuilder.withActorSystem(system).build
    try {
      kryoSerializeAndDeserialize((1, "second", 3.0))
    } finally {
      g.shutdown
      system.shutdown()
    }
  }

  def kryoSerializeAndDeserialize(instance: AnyRef) {
    val akka = ActorSystemRegistry.retrieve("SignalCollect").get
    val serialization = SerializationExtension(akka)
    val s = serialization.findSerializerFor(instance)
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

  "DefaultSerializer" should "correctly serialize/deserialize a Double" in {
    DefaultSerializer.read[Double](DefaultSerializer.write(1024.0)) shouldBe 1024.0
  }

  it should "correctly serialize/deserialize a job configuration" in {
    val job = new Job(
      100,
      Some(SpreadsheetConfiguration("some.emailAddress@gmail.com", "somePasswordHere", "someSpreadsheetNameHere", "someWorksheetNameHere")),
      "someUsername",
      "someJobDescription")
    DefaultSerializer.read[Job](DefaultSerializer.write(job)) shouldBe job
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