/*
 *  @author Daniel Strebel
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

package com.signalcollect.implementations.graph

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.signalcollect._

@RunWith(classOf[JUnitRunner])
class GraphModificationSpec extends SpecificationWithJUnit {

  "GraphEditor" should {

    "remove all vertices that satisfy a condition" in {

      val g = GraphBuilder.build
      g.addVertex(new GraphModificationVertex(0, 1))
      g.addVertex(new GraphModificationVertex(1, 1))
      g.addVertex(new GraphModificationVertex(2, 1))

      g.execute
      g.removeVertices(v => (v.asInstanceOf[GraphModificationVertex].id % 2 == 0))
      g.aggregate(new CountVertices[GraphModificationVertex]) === 1
    }
    
    "remove all vertices that satisfy a condition" in {

      val g = GraphBuilder.build
      g.addVertex(new GraphModificationVertex(0, 1))
      g.addVertex(new GraphModificationVertex(1, 1))
      g.addVertex(new GraphModificationVertex(2, 1))

      g.execute
      g.removeVertex(2, true)
      g.removeVertex(1, true)
      g.aggregate(new CountVertices[GraphModificationVertex]) === 1
    }
  }
}

class GraphModificationVertex(id: Int, state: Int) extends DataGraphVertex(id, state) {
  type Signal = Int
  def collect(oldState: State, mostRecentSignals: Iterable[Int]): Int = {
    1
  }
}