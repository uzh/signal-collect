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
import scala.reflect.ClassTag

/**
 *  Storage backed by a memory efficient VertexMap.
 *  Inserts/removals of vertices are slower than with a Java HashMap.
 */
class MemoryEfficientStorage[@specialized(Int, Long) Id: ClassTag, Signal: ClassTag] extends StorageFactory[Id, Signal] {
  def createInstance: Storage[Id, Signal] = {
    val idClass = implicitly[ClassTag[Id]].runtimeClass.asInstanceOf[Class[Id]]
    val signalClass = implicitly[ClassTag[Signal]].runtimeClass.asInstanceOf[Class[Signal]]
    val int = classOf[Int]
    val long = classOf[Long]
    val float = classOf[Float]
    val double = classOf[Double]
    val any = classOf[Any]
    val storage: Storage[Id, Signal] = {
      if (idClass == int && signalClass == int) {
        new IntIntVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == int && signalClass == long) {
        new IntLongVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == int && signalClass == float) {
        new IntFloatVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == int && signalClass == double) {
        new IntDoubleVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == int && signalClass == any) {
        new IntAnyVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == long && signalClass == int) {
        new LongIntVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == long && signalClass == long) {
        new LongLongVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == long && signalClass == float) {
        new LongFloatVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == long && signalClass == double) {
        new LongDoubleVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == long && signalClass == any) {
        new LongAnyVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == any && signalClass == int) {
        new AnyIntVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == any && signalClass == long) {
        new AnyLongVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == any && signalClass == float) {
        new AnyFloatVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else if (idClass == any && signalClass == double) {
        new AnyDoubleVertexMapStorage().asInstanceOf[Storage[Id, Signal]]
      } else {
        new VertexMapStorage[Id, Signal]() // Rely on specialization ... good luck with that. :)
      }
    }
    storage
  }
  override def toString = "MemoryEfficientStorage"
}
