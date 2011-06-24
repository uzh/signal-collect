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

trait Configuration {
  def executionMode: ExecutionMode
  def numberOfWorkers: Int
  def messageBusFactory: MessageBusFactory
  def storageFactory: StorageFactory
  def optionalLogger: Option[MessageRecipient[Any]]
}

trait Factory extends Serializable {
  def name: String = this.getClass.getSimpleName
}

trait WorkerFactory extends Factory {
  def createInstance(workerId: Int, messageBus: MessageBus[Any,Any], storageFactory: StorageFactory): Worker
}

trait MessageBusFactory extends Factory {
  def createInstance(numberOfWorkers: Int, mapper: VertexToWorkerMapper): MessageBus[Any, Any]
}

trait StorageFactory extends Factory {
  def createInstance(messageBus: MessageBus[Any, Any]): Storage
}

sealed trait ExecutionMode extends Serializable

object SynchronousExecutionMode extends ExecutionMode {
  override def toString = "SynchronousExecutionMode"
}
object AsynchronousExecutionMode extends ExecutionMode {
  override def toString = "AsynchronousExecutionMode"
}