/*
 *  @author Philip Stutz
 *
 *  Copyright 2012 University of Zurich
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

package com.signalcollect.storage

import com.signalcollect.Vertex
import com.signalcollect.interfaces.VertexStore
import StorageDefaultValues._

object StorageDefaultValues {
  def defaultInitialSize = 32768
  def defaultRehashFraction = 0.75f
  def defaultVertexMapRehashFraction = 0.8f
  def defaultTemporaryMapRehashFraction = 0.9f
  def defaultToSignalInitialSize = 1024
}

/**
 * Specialization does not work well, so we manually specialize for frequent ID/Signal configurations.
 */
final class IntDoubleVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Int, Double](initialSize, rehashFraction)

final class IntFloatVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Int, Float](initialSize, rehashFraction)

final class IntIntVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Int, Int](initialSize, rehashFraction)

final class IntLongVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Int, Long](initialSize, rehashFraction)

final class LongDoubleVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Long, Double](initialSize, rehashFraction)

final class LongFloatVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Long, Float](initialSize, rehashFraction)

final class LongIntVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Long, Int](initialSize, rehashFraction)

final class LongLongVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Long, Long](initialSize, rehashFraction)

final class AnyDoubleVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Any, Double](initialSize, rehashFraction)

final class AnyFloatVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Any, Float](initialSize, rehashFraction)

final class AnyIntVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Any, Int](initialSize, rehashFraction)

final class AnyLongVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Any, Long](initialSize, rehashFraction)

final class IntAnyVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Int, Any](initialSize, rehashFraction)

final class LongAnyVertexMap(
  initialSize: Int = defaultInitialSize,
  rehashFraction: Float = defaultRehashFraction) extends VertexMap[Long, Any](initialSize, rehashFraction)
