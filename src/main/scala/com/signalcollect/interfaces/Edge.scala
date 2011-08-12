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

package com.signalcollect.interfaces

case class DefaultEdgeId[SourceId, TargetId](sourceId: SourceId, targetId: TargetId, description: String = "") extends EdgeId[SourceId, TargetId] 

trait EdgeId[+SourceId, +TargetId] extends Serializable {
  def sourceId: SourceId
  def targetId: TargetId
  def description: String
}

trait Edge extends Serializable {
  
  /** The type of the source {@link Vertex} which can be found using {@link #sourceId}. */
  type SourceVertex <: Vertex
  type SourceId
  type TargetId
  type Signal
  
  /** The identifier of this {@link Edge}. */
  def id: EdgeId[SourceId, TargetId]
  
  /** called when the edge is attached to a source vertex */
  def onAttach(sourceVertex: SourceVertex) = {}
  
  /** The weight of this {@link Edge}. By default an {@link Edge} has a weight of <code>1</code>. */
  def weight: Double = 1

  /** A textual representation of this {@link Edge}. */
  override def toString = getClass.getSimpleName + "(sourceId=" + id.sourceId + ", targetId=" + id.targetId + ")"
  
  /** The hash code of this object. */
  override def hashCode = id.hashCode

  override def equals(other: Any): Boolean = {
    if (other.isInstanceOf[Edge]) {
      id.equals(other.asInstanceOf[Edge].id)
    } else {
      false
    }
  }
 
  /**
   * This method will be called by {@link FrameworkVertex#executeSignalOperation}
   * of this {@Edge} source vertex. It calculates the signal and sends it over the message bus.
   * {@link OnlySignalOnChangeEdge}.
   * 
   * @param mb the message bus to use for sending the signal
   * @param souceVertex the source vertex to get the state to assemble the signal
   */
  def executeSignalOperation(sourceVertex: Vertex, mb: MessageBus[Any])

}