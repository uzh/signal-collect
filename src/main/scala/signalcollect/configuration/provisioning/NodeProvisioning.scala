/*
 *  @author Francisco de Freitas
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

package signalcollect.configuration.provisioning

import scala.collection.mutable.HashMap

/**
 * Provisioning for nodes
 * 
 * How many workers per node
 * TODO: vertices per worker
 * TODO: segments per worker
 * 
 */
trait NodeProvisioning {

  protected def workersPerNode: HashMap[String, Int]

  def workerPorts: HashMap[String, List[Int]]

  /**
   * TODO: doesnt really work if we are using hashes to locate vertices in the workers, however that could change
   *
   * def verticesPerWorker: Map[Int, Long]
   *
   * def segmentsPerWorker: Map[Int, Int]
   */

}