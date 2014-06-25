/*
 *  @author Philip Stutz
 *  @author Tobias Bachmann
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

package com.signalcollect.deployment

import com.signalcollect.util.FileDownloader
import com.signalcollect.GraphBuilder
import java.net.URL
import scala.concurrent._
import java.io.FileInputStream
import java.io.DataInputStream
import com.signalcollect.examples.EfficientPageRankVertex
import com.signalcollect.examples.PlaceholderEdge
import com.signalcollect.util.Ints
import java.io.FileReader
import java.io.BufferedReader
import com.signalcollect.GraphEditor
import scala.collection.mutable.ArrayBuffer
import com.signalcollect.Vertex
import com.signalcollect.Edge
import com.signalcollect.factory.messagebus.BulkAkkaMessageBusFactory

object Handlers {
  def nonExistingVertex: (Edge[Any], Any) => Option[Vertex[Any, _]] = {
    (edgedId, vertexId) => Some(new EfficientPageRankVertex(vertexId.asInstanceOf[Int]))
  }
  def undeliverableSignal: (Any, Any, Option[Any], GraphEditor[Any, Any]) => Unit = {
    case (signal, id, sourceId, ge) =>
      ge.addVertex(new EfficientPageRankVertex(id.asInstanceOf[Int]))
      ge.sendSignal(signal, id, sourceId)
  }
}