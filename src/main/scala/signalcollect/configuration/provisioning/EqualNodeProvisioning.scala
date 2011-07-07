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
import signalcollect.util.Constants

/**
 *
 * Equal node provisioning will put the same amount of workers in every node.
 * In case in case it's and odd number of workers and nodes, the first node gets one more worker than the others
 * eg. 25 workers in 3 nodes = 9 on the first, 8 on the other 2
 */
class EqualNodeProvisioning(nodesAddress: Vector[String], numberOfWorkers: Int) extends NodeProvisioning {

  override def toString = "EqualNodeProvisioning"

  protected def workersPerNode: HashMap[String, Int] = {

    //val nodesAddress = bootstrapConfig.nodesAddress
    val numberOfNodes = nodesAddress.size
    //val numberOfWorkers = config.numberOfWorkers

    val workersPerNode = new HashMap[String, Int]

    // equal division of workers among all nodes
    val div = numberOfWorkers.asInstanceOf[Double] / numberOfNodes.asInstanceOf[Double]

    // for each node address
    for (i <- 0 to numberOfNodes) {
      // in case its and odd number of workers and nodes, the first node gets one more worker than the others
      if (i == 0)
        workersPerNode += nodesAddress(i) -> (math.ceil(div).asInstanceOf[Int])
      else
        workersPerNode += nodesAddress(i) -> (math.ceil(div).asInstanceOf[Int])
    }

    workersPerNode

  }

  /**
   * Returns a map containing the ports where the workers will be listening, per ip address
   */
  def workerPorts: HashMap[String, List[Int]] = {

    // e.g.: 192.168.1.1 -> List(2554,2555)
    val workers = new HashMap[String, List[Int]]

    // iterator for map
    val it = workersPerNode.iterator

    // for each node ip address
    while (it.hasNext) {

      // in the node ip there will be x workers
      val next = it.next

      // the last port used
      val cap: Int = Constants.WORKER_PORT_RANGE_START + next._2

      // where the workers should be listening
      var ports = List[Int]()

      // from highest port to lowest port (to keep the order of the list)
      for (i <- cap to Constants.WORKER_PORT_RANGE_START by -1)
        ports = i :: ports

      // put address and ports in the map
      workers.put(next._1, ports)

    }

    workers

  }

}