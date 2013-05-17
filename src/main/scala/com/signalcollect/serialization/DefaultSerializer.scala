/*
 *  @author Daniel Strebel
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
 */

package com.signalcollect.serialization

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Companion object for DefaultSerializer
 */
object DefaultSerializer extends DefaultSerializer

/**
 * Default Serializer that uses standard java.io ObjectIn- and ObjectOutputStreams
 * and can serialize any object declared as serializable.
 */
trait DefaultSerializer {

  /**
   * Serializes an object.
   *
   * @param inputObject the object to serialize
   * @return serialized object as byte array
   */
  def write[A](inputObject: A): Array[Byte] = {
    val barr = new ByteArrayOutputStream(8192)
    val out = new ObjectOutputStream(barr)
    out.writeObject(inputObject)
    out.close
    barr.toByteArray
  }

  /**
   * Deserialize an object.
   *
   * @param the serialized object as byte array
   * @return the deserialized object
   */
  def read[A](buffer: Array[Byte]): A = {
    val input = new ObjectInputStream(new ByteArrayInputStream(buffer))
    val obj = input.readObject
    input.close
    obj.asInstanceOf[A]
  }
}