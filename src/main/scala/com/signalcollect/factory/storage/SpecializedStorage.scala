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

package com.signalcollect.factory.storage

import com.signalcollect.interfaces.Storage
import com.signalcollect.interfaces.StorageFactory
import com.signalcollect.storage._

/**
 * Specialization does not work well, so we manually specialize for frequent ID/Signal configurations.
 */
object IntDoubleStorageFactory extends StorageFactory[Int, Double] {
  def createInstance = new IntDoubleVertexMapStorage
}

object IntFloatStorageFactory extends StorageFactory[Int, Float] {
  def createInstance = new IntFloatVertexMapStorage
}

object IntIntStorageFactory extends StorageFactory[Int, Int] {
  def createInstance = new IntIntVertexMapStorage
}

object IntLongStorageFactory extends StorageFactory[Int, Long] {
  def createInstance = new IntLongVertexMapStorage
}

object LongDoubleStorageFactory extends StorageFactory[Long, Double] {
  def createInstance = new LongDoubleVertexMapStorage
}

object LongFloatStorageFactory extends StorageFactory[Long, Float] {
  def createInstance = new LongFloatVertexMapStorage
}

object LongIntStorageFactory extends StorageFactory[Long, Int] {
  def createInstance = new LongIntVertexMapStorage
}

object LongLongStorageFactory extends StorageFactory[Long, Long] {
  def createInstance = new LongLongVertexMapStorage
}

object AnyDoubleStorageFactory extends StorageFactory[Any, Double] {
  def createInstance = new AnyDoubleVertexMapStorage
}

object AnyFloatStorageFactory extends StorageFactory[Any, Float] {
  def createInstance = new AnyFloatVertexMapStorage
}

object AnyIntStorageFactory extends StorageFactory[Any, Int] {
  def createInstance = new AnyIntVertexMapStorage
}

object AnyLongStorageFactory extends StorageFactory[Any, Long] {
  def createInstance = new AnyLongVertexMapStorage
}

object IntAnyStorageFactory extends StorageFactory[Int, Any] {
  def createInstance = new IntAnyVertexMapStorage
}

object LongAnyStorageFactory extends StorageFactory[Long, Any] {
  def createInstance = new LongAnyVertexMapStorage
}
