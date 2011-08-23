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

package com.signalcollect.javaapi

import com.signalcollect.Edge
import com.signalcollect.Vertex
import com.signalcollect.DefaultEdgeId
import com.signalcollect.EdgeId
import com.signalcollect.interfaces.SignalMessage
import scala.reflect.BeanProperty
import com.signalcollect.interfaces.MessageBus

/**
 *  A version of edge that serves as the foundation for the Java API edges.
 *  It translates type parameters to Scala type members, which are unavailable in Java.
 *
 *  @note The bean property annotation tells the compiler to generate getters and setters, which makes fields
 *  more easily accessible from Java.
 */
class DefaultEdge[SourceVertexTypeParameter <: Vertex](
  sourceId: Object,
  targetId: Object,
  description: String,
  @BeanProperty val weight: Double) extends Edge {

  def this(sourceId: Object, targetId: Object, description: String) = this(sourceId, targetId, description, 1.0)
  def this(sourceId: Object, targetId: Object) = this(sourceId, targetId, "", 1.0)

  type SourceId = Object
  type TargetId = Object
  type SourceVertex = SourceVertexTypeParameter
  type Signal = Object

  @BeanProperty val id: EdgeId[Object, Object] = DefaultEdgeId[Object, Object](sourceId, targetId, "")

  val cachedTargetIdHashCode = id.targetId.hashCode

  def executeSignalOperation(sourceVertex: Vertex, mb: MessageBus[Any]) {
    mb.sendToWorkerForVertexIdHash(SignalMessage(id, signal(sourceVertex.asInstanceOf[SourceVertex])), cachedTargetIdHashCode)
  }

  def signal(sourceVertex: SourceVertex): Object = null

}