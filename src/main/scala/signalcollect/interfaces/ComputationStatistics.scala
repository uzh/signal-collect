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

package signalcollect.interfaces

trait ComputationStatistics {
  def numberOfWorkers: Option[Int]
  def computationTimeInMilliseconds: Option[Long]
  def jvmCpuTimeInMilliseconds: Option[Long]
  def graphLoadingWaitInMilliseconds: Option[Long]
  def computeGraph: Option[String]
  def storage: Option[String]
  def worker: Option[String]
  def messageBus: Option[String]
  def messageInbox: Option[String]
  def logger: Option[String]
  def signalCollectSteps: Option[Long]
  def numberOfVertices: Option[Long]
  def numberOfEdges: Option[Long]
  def vertexCollectOperations: Option[Long]
  def vertexSignalOperations: Option[Long]
  def stepsLimit: Option[Long]
  def signalThreshold: Option[Double]
  def collectThreshold: Option[Double]
  def stallingDetectionCycles: Option[Long]
}