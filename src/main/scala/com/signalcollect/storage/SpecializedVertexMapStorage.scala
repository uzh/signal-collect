/*
 *  @author Daniel Strebel
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
 */

package com.signalcollect.storage

import com.signalcollect.interfaces.Storage
import com.signalcollect.interfaces.VertexStore
import StorageDefaultValues._


/**
 * Specialization does not work well, so we manually specialize for frequent ID/Signal configurations.
 */
final class IntDoubleVertexMapStorage
  extends Storage[Int, Double] {
  val vertices = new IntDoubleVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new IntDoubleVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new IntDoubleVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class IntFloatVertexMapStorage
  extends Storage[Int, Float] {
  val vertices = new IntFloatVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new IntFloatVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new IntFloatVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class IntIntVertexMapStorage
  extends Storage[Int, Int] {
  val vertices = new IntIntVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new IntIntVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new IntIntVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class IntLongVertexMapStorage
  extends Storage[Int, Long] {
  val vertices = new IntLongVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new IntLongVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new IntLongVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class LongDoubleVertexMapStorage
  extends Storage[Long, Double] {
  val vertices = new LongDoubleVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new LongDoubleVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new LongDoubleVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class LongFloatVertexMapStorage
  extends Storage[Long, Float] {
  val vertices = new LongFloatVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new LongFloatVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new LongFloatVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class LongIntVertexMapStorage
  extends Storage[Long, Int] {
  val vertices = new LongIntVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new LongIntVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new LongIntVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class LongLongVertexMapStorage
  extends Storage[Long, Long] {
  val vertices = new LongLongVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new LongLongVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new LongLongVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class AnyDoubleVertexMapStorage
  extends Storage[Any, Double] {
  val vertices = new AnyDoubleVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new AnyDoubleVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new AnyDoubleVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}


final class AnyFloatVertexMapStorage
  extends Storage[Any, Float] {
  val vertices = new AnyFloatVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new AnyFloatVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new AnyFloatVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class AnyIntVertexMapStorage
  extends Storage[Any, Int] {
  val vertices = new AnyIntVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new AnyIntVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new AnyIntVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class AnyLongVertexMapStorage
  extends Storage[Any, Long] {
  val vertices = new AnyLongVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new AnyLongVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new AnyLongVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class IntAnyVertexMapStorage
  extends Storage[Int, Any] {
  val vertices = new IntAnyVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new IntAnyVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new IntAnyVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}

final class LongAnyVertexMapStorage
  extends Storage[Long, Any] {
  val vertices = new LongAnyVertexMap(rehashFraction = defaultVertexMapRehashFraction)
  val toCollect = new LongAnyVertexMap(rehashFraction = defaultTemporaryMapRehashFraction)
  val toSignal = new LongAnyVertexMap(initialSize = defaultToSignalInitialSize, rehashFraction = defaultTemporaryMapRehashFraction)
}
