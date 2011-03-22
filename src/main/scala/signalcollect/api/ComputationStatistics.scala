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

package signalcollect.api

import scala.collection.mutable

class ComputationStatistics(statsMap: mutable.Map[String, Any] = mutable.LinkedHashMap[String, Any]()) {
  def numberOfWorkers: Option[Int] = statsMap.get("numberOfWorkers") collect { case x:Int => x }
  def computationTimeInMilliseconds: Option[Long] = statsMap.get("computationTimeInMilliseconds") collect { case x:Long => x }
  def jvmCpuTimeInMilliseconds: Option[Long] = statsMap.get("jvmCpuTimeInMilliseconds") collect { case x:Long => x }
  def computeGraph: Option[String] = statsMap.get("computeGraph") collect { case x:String => x }
  def worker: Option[String] = statsMap.get("worker") collect { case x:String => x }
  def messageBus: Option[String] = statsMap.get("messageBus") collect { case x:String => x }
  def messageInbox: Option[String] = statsMap.get("messageInbox") collect { case x:String => x }
  def logger: Option[String] = statsMap.get("logger") collect { case x:String => x }
  def signalCollectSteps: Option[Long] = statsMap.get("signalCollectSteps") collect { case x:Long => x }
  def numberOfVertices: Option[Long] = statsMap.get("numberOfVertices") collect { case x:Long => x }
  def numberOfEdges: Option[Long] = statsMap.get("numberOfEdges") collect { case x:Long => x }
  def vertexCollectOperations: Option[Long] = statsMap.get("vertexCollectOperations") collect { case x:Long => x }
  def vertexSignalOperations: Option[Long] = statsMap.get("vertexSignalOperations") collect { case x:Long => x }
  
  override def toString = statsMap.foldLeft(""){ (aggr, mapEntry) => aggr + "\n" + mapEntry._1 + ": " + mapEntry._2 }
}