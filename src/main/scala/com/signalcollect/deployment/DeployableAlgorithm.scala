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

import com.signalcollect.Graph
import com.signalcollect.GraphBuilder

import akka.actor.ActorRef
import akka.actor.ActorSystem

/**
 * implement your algorithm with this trait to deploy it to a cluster
 */
trait DeployableAlgorithm {
  
  /**
   * this function should be called by an implementation of a Cluster (like in the @see com.signalcollect.deployment.DefaultLeader)
   * it already creates a GraphBuilder, which is passed to the abstract execute
   * @param parameters are the parameters defined in the deployment.conf
   * @param nodeActors are the nodeActors which should be provided by the cluster
   * @param actorSystem if already an actorSystem is defined, it should be passed in, so that not a new one has to be created
   */
  def execute(parameters: Map[String, String], nodeActors: Option[Array[ActorRef]], actorSystem: Option[ActorSystem] = None) {
    val g1 = if (actorSystem.isDefined)
      GraphBuilder.withActorSystem(actorSystem.get)
    else GraphBuilder
    val g2 = if (nodeActors.isDefined)
      g1.withPreallocatedNodes(nodeActors.get)
    else
      g1
    execute(parameters, g2)
  }
  /**
   * this function must be implemented by an alogrithm to deploy it to a cluster. 
   * @param parameters are the parameters defined in the deployment.conf
   * @param graphBuilder is a graph builder, which contains the preallocated nodes of the cluster
   */
  def execute(parameters: Map[String, String], graphBuilder: GraphBuilder[Any, Any])

}
