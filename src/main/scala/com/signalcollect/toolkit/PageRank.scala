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
 *
 */

package com.signalcollect.toolkit

import com.signalcollect._
import com.signalcollect.examples._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.factory.messagebus.IntIdDoubleSignalMessageBusFactory
import com.signalcollect.loading._
import com.signalcollect.util.FileReader

/** Builds a PageRank compute graph and executes the computation */
object PageRank extends App {
  val graph = new GraphBuilder[Int, Double]().
    withMessageBusFactory(new IntIdDoubleSignalMessageBusFactory(10000)).
    build
  // Substituting ID 0.
  val substitutingIterator = FileReader.intIterator(s"./${args(0)}").map { id =>
    assert(id != Int.MaxValue, "ID Int.MaxValue is not supported.")
    if (id == 0) Int.MaxValue else id
  }
  val vertexData = new VertexTupleIterator(substitutingIterator)
  def vertexCreator(id: Int, targetIds: List[Int]) = {
    val v = new MemoryMinimalPrecisePage(id)
    v.setTargetIdArray(targetIds.toArray)
    v
  }
  val loader = Loading.loader(vertexData, vertexCreator)
  graph.loadGraph(loader, Some(0))
  graph.awaitIdle
  val stats = graph.execute
  println(stats)
  //TODO: Substitute Int.MaxValue back to 0
  graph.shutdown
}
