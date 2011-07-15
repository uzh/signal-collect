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

package signalcollect.implementations.graph

import signalcollect.interfaces._

abstract class AbstractEdge[SourceIdType, TargetIdType] extends Edge[SourceIdType, TargetIdType] with Serializable {

  /** The identifier of the {@link Vertex} where this {@link Edge} originates from. */
  val sourceId: SourceIdType

  /** The identifier of the {@link Vertex} where this {@link Edge} points to. */
  val targetId: TargetIdType

  /**
   * The abstract "signal" function is algorithm specific and has to be implemented by a user of the API
   * this function will be called during algorithm execution. It is meant to calculate a signal
   * going from the source vertex of this edge to the target vertex of this edge.
   */
  def signal(sourceVertex: SourceVertexType): SignalType

  /** The weight of this {@link Edge}. By default an {@link Edge} has a weight of <code>1</code>. */
  def weight: Double = 1

  /** The identifier of this {@link Edge}. */
  override val id = (sourceId, targetId, getClass.getSimpleName)

  /** The hash code of this object. */
  override val hashCode = id.hashCode

  override def equals(other: Any): Boolean = {
    if (other.isInstanceOf[Edge[_, _]]) {
      id.equals(other.asInstanceOf[Edge[_, _]].id)
    } else {
      false
    }
  }
  
  /** The hash code of the target vertex. */
  override val targetHashCode = targetId.hashCode

  /** A textual representation of this {@link Edge}. */
  override def toString = this.getClass.getSimpleName + "(" + sourceId + ", " + targetId + ")"

  /** called when the edge is attached to a source vertex */
  def onAttach(sourceVertex: SourceVertexType) = {}

  /**
   * This method will be called by {@link FrameworkVertex#executeSignalOperation}
   * of this {@Edge} source vertex. It calculates the signal and sends it over the message bus.
   * {@link OnlySignalOnChangeEdge}.
   * 
   * @param mb the message bus to use for sending the signal
   */
  def executeSignalOperation(sourceVertex: Vertex[_,_], mb: MessageBus[Any]) {
      mb.sendToWorkerForVertexIdHash(Signal(sourceId, targetId, signal(sourceVertex.asInstanceOf[SourceVertexType])), targetHashCode)
  }

}