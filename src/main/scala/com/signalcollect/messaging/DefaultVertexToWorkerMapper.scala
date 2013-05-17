/*
 *  @author Philip Stutz
 *
 *  Copyright 2010 University of Zurich
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

package com.signalcollect.messaging

import com.signalcollect.interfaces.VertexToWorkerMapper

class DefaultVertexToWorkerMapper[Id](numberOfWorkers: Int) extends VertexToWorkerMapper[Id] with Serializable {
  def getWorkerIdForVertexId(vertexId: Id): Int = getWorkerIdForVertexIdHash(vertexId.hashCode)

  //  def getWorkerIdForVertexIdHash(vertexIdHash: Int): Int = (vertexIdHash % numberOfWorkers).abs (BAD PERFORMANCE!)
  def getWorkerIdForVertexIdHash(vertexIdHash: Int): Int = {
    val workerId = vertexIdHash % numberOfWorkers
    if (workerId >= 0) {
      workerId
    } else {
      if (workerId == Int.MinValue) {
        // Special case,-Int.MinValue == Int.MinValue
        0
      } else {
        -workerId
      }
    }
  }
}
