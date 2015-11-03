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

import com.signalcollect.{ TestConfig, GraphBuilder }
import akka.serialization.SerializationExtension
import com.romix.akka.serialization.kryo.KryoSerializer
import org.scalatest.Matchers
import org.scalatest.fixture.FlatSpec
import TestConfig._
import akka.actor.ActorSystem
import com.signalcollect.TestGraph
import org.scalatest.fixture.UnitFixture
import org.scalatest.fixture.NoArg

class SerializerSpec extends FlatSpec with Matchers with UnitFixture {

  "Kryo" should "correctly serialize Scala immutable maps" in new TestGraph {
    // Scala uses special representations for small maps.
    kryoSerializeAndDeserialize(Map.empty[Int, Double])
    kryoSerializeAndDeserialize(Map(1 -> 1.5))
    kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4))
    kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5))
    kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2))
    kryoSerializeAndDeserialize(Map(1 -> 1.5, 2 -> 5.4, 3 -> 4.5, 4 -> 1.2, 6 -> 3.2))
  }

  it should "correctly serialize Scala immutable sets" in new TestGraph {
    // Scala uses special representations for small sets.
    kryoSerializeAndDeserialize(Set.empty[Int])
    kryoSerializeAndDeserialize(Set(1))
    kryoSerializeAndDeserialize(Set(1, 2))
    kryoSerializeAndDeserialize(Set(1, 2, 3))
    kryoSerializeAndDeserialize(Set(1, 2, 3, 4))
    kryoSerializeAndDeserialize(Set(1, 2, 3, 4, 5))
  }

  it should "correctly serialize Scala None" in new TestGraph {
    kryoSerializeAndDeserialize(None)
  }

  it should "correctly serialize Scala List" in new TestGraph {
    kryoSerializeAndDeserialize(List.empty[Int])
    kryoSerializeAndDeserialize(List(1))
  }

  it should "correctly serialize Scala Vector" in new TestGraph {
    kryoSerializeAndDeserialize(Vector.empty[Int])
    kryoSerializeAndDeserialize(Vector(1))
  }

  it should "correctly serialize Scala Seq" in new TestGraph {
    kryoSerializeAndDeserialize(Seq.empty[Int])
    kryoSerializeAndDeserialize(Seq(1))
  }

  it should "correctly serialize Scala Array" in new TestGraph {
    assert(kryoSerializeAndDeserializeSpecial(Array.empty[Int]).toList == List())
    assert(kryoSerializeAndDeserializeSpecial(Array(1)).toList == List(1))
    assert(kryoSerializeAndDeserializeSpecial(Array(1.0)).toList == List(1.0))
    assert(kryoSerializeAndDeserializeSpecial(Array(1l)).toList == List(1l))
    assert(kryoSerializeAndDeserializeSpecial(Array("abc")).toList == List("abc"))
  }

  it should "correctly serialize Array[Array[Int]]" in new TestGraph {
    assert(kryoSerializeAndDeserializeSpecial(
      Array(Array(1, 2, 3), Array(3, 4, 5))).map(_.toList).toList == List(List(1, 2, 3), List(3, 4, 5)))
  }

  it should "correctly serialize integers" in new TestGraph {
    kryoSerializeAndDeserialize(Integer.valueOf(1))
  }

  it should "correctly serialize longs" in new TestGraph {
    kryoSerializeAndDeserialize(Long.box(1l))
  }

  it should "correctly serialize floats" in new TestGraph {
    kryoSerializeAndDeserialize(Float.box(1.0f))
  }

  it should "correctly serialize doubles" in new TestGraph {
    kryoSerializeAndDeserialize(Double.box(1.0d))
  }

  it should "correctly serialize booleans" in new TestGraph {
    kryoSerializeAndDeserialize(Boolean.box(true))
  }

  it should "correctly serialize shorts" in new TestGraph {
    kryoSerializeAndDeserialize(Short.box(1))
  }

  it should "correctly serialize strings" in new TestGraph {
    kryoSerializeAndDeserialize("abc")
  }

  it should "correctly serialize Java strings" in new TestGraph {
    val javaString: java.lang.String = "abc"
    kryoSerializeAndDeserialize(javaString)
  }

  it should "correctly serialize Tuple2" in new TestGraph {
    kryoSerializeAndDeserialize((1, "second"))
  }

  it should "correctly serialize Tuple3" in new TestGraph {
    kryoSerializeAndDeserialize((1, "second", 3.0))
  }

  def kryoSerializeAndDeserialize(instance: AnyRef)(implicit system: ActorSystem) {
    val serialization = SerializationExtension(system)
    val s = serialization.findSerializerFor(instance)
    assert(s.isInstanceOf[KryoSerializer])
    val bytes = s.toBinary(instance)
    val b = s.fromBinary(bytes, manifest = None)
    assert(b == instance)
  }

  def kryoSerializeAndDeserializeSpecial[T <: AnyRef](instance: T)(implicit system: ActorSystem): T = {
    val serialization = SerializationExtension(system)
    val s = serialization.findSerializerFor(instance)
    assert(s.isInstanceOf[KryoSerializer])
    val bytes = s.toBinary(instance)
    val b = s.fromBinary(bytes, manifest = None).asInstanceOf[T]
    b
  }

  "DefaultSerializer" should "correctly serialize/deserialize a Double" in new NoArg {
    DefaultSerializer.read[Double](DefaultSerializer.write(1024.0)) shouldBe 1024.0
  }

  it should "correctly serialize/deserialize a job configuration" in new NoArg {
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