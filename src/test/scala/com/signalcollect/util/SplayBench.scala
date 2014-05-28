/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

package com.signalcollect.util

import scala.util.Random

object SplayBench extends App {
  val overheadFractions = List(0.0f, 0.05f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 1.0f)
  val splitSize = List(10, 100, 1000, 10000)//, 100000)
  val splayIntSets = for {
    overhead <- overheadFractions
    split <- splitSize
  } yield new SimpleSplayIntSet(overhead, split)

  def operation(s: SplayIntSet)(): Unit = {
    s.insert(Random.nextInt)
  }
  val stats = splayIntSets.map(
    splay => (operation(splay) _, splay.toString)).map {
      case (op, desc) => Benchmark(op, desc, measuredRepetitions = 1000000)
    }
  stats.sortBy(_.nanosecondsPerExecution).foreach(println)
}
