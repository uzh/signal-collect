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

import signalcollect.interfaces.ComputationStatistics
import scala.collection.mutable

class DefaultComputationStatistics(statsMap: Map[String, Any]) extends ComputationStatistics {
  def numberOfWorkers: Option[Int] = statsMap.get("numberOfWorkers") collect { case x:Int => x }
  def computationTimeInMilliseconds: Option[Long] = statsMap.get("computationTimeInMilliseconds") collect { case x:Long => x }
  def jvmCpuTimeInMilliseconds: Option[Long] = statsMap.get("jvmCpuTimeInMilliseconds") collect { case x:Long => x }
  def graphLoadingWaitInMilliseconds: Option[Long] = statsMap.get("graphLoadingWaitInMilliseconds") collect { case x:Long => x }
  def executionMode: Option[String] = statsMap.get("executionMode") collect { case x:String => x }
  def storage: Option[String] = statsMap.get("storage") collect { case x:String => x }
  def worker: Option[String] = statsMap.get("worker") collect { case x:String => x }
  def messageBus: Option[String] = statsMap.get("messageBus") collect { case x:String => x }
  def logger: Option[String] = statsMap.get("logger") collect { case x:String => x }
  def signalCollectSteps: Option[Long] = statsMap.get("signalCollectSteps") collect { case x:Long => x }
  def numberOfVertices: Option[Long] = statsMap.get("numberOfVertices") collect { case x:Long => x }
  def verticesAdded: Option[Long] = statsMap.get("verticesAdded") collect { case x:Long => x }
  def verticesRemoved: Option[Long] = statsMap.get("verticesRemoved") collect { case x:Long => x }
  def numberOfEdges: Option[Long] = statsMap.get("numberOfEdges") collect { case x:Long => x }
  def edgesAdded: Option[Long] = statsMap.get("edgesAdded") collect { case x:Long => x }
  def edgesRemoved: Option[Long] = statsMap.get("edgesRemoved") collect { case x:Long => x }
  def vertexCollectOperations: Option[Long] = statsMap.get("vertexCollectOperations") collect { case x:Long => x }
  def collectOperationsPending: Option[Long] = statsMap.get("collectOperationsPending") collect { case x:Long => x }
  def vertexSignalOperations: Option[Long] = statsMap.get("vertexSignalOperations") collect { case x:Long => x }
  def signalOperationsPending: Option[Long] = statsMap.get("signalOperationsPending") collect { case x:Long => x }
  def stepsLimit: Option[Long] = statsMap.get("stepsLimit") collect { case x:Long => x }
  def signalThreshold: Option[Double] = statsMap.get("signalThreshold") collect { case x:Double => x }
  def collectThreshold: Option[Double] = statsMap.get("collectThreshold") collect { case x:Double => x }
  def stallingDetectionCycles: Option[Long] = statsMap.get("stallingDetectionCycles") collect { case x:Long => x }
  
  override def toString = statsMap.foldLeft(""){ (aggr, mapEntry) => aggr + "\n" + mapEntry._1 + ": " + mapEntry._2 }
}