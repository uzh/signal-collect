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
import scala.collection.immutable.HashMap
import com.signalcollect.ExecutionInformation
import com.signalcollect.ExecutionInformation
import com.signalcollect.PrivateGraph

/**
 * implement your algorithm with this class to deploy it to a cluster
 */
abstract class Algorithm extends App {
  private var logger: SimpleLogger = SimpleConsoleLogger
  run

  def deploy: Boolean = false

  /**
   * logger which can be dynamically exchanged
   */
  def log: SimpleLogger = {
    if (logger == null) logger = SimpleConsoleLogger //make sure log is not null
    logger
  }

  /**
   * runs the algorithm locally if it is started as App
   */
  def run = {
    if (startedFromApp && !deploy) {
      println("started from App -not deploy")
      lifecycle()
    }
    if (startedFromApp && deploy) {
      println("started from App -deploy")
      deployToCluster
    }

  }

  /**
   * detects if algorithm is started as App
   */
  def startedFromApp: Boolean = {
    args != null // if args exists it must be started as App
  }

  /**
   * deploys this algorithm to a cluster specified in the deployment.conf
   */
  def deployToCluster {
    val deploymentConf: DeploymentConfiguration = DeploymentConfigurationCreator.getDeploymentConfiguration
      .asInstanceOf[BasicDeploymentConfiguration]
      .copy(algorithm = this.getClass.getName)
    val cluster = ClusterCreator.getCluster(deploymentConf)
    cluster.deploy(deploymentConf)
  }

  /**
   * defines a default lifecycle of an algorithm
   */
  def lifecycle(parameters: Map[String, String] = new HashMap[String, String],
    nodeActors: Option[Array[ActorRef]] = None,
    actorSystem: Option[ActorSystem] = None) {
    logger = changeLogger(actorSystem)
    beforeStart
    val defaultGraphBuilder = createDefaultGraphBuilder(nodeActors, actorSystem)
    val configuredGraphBuilder = configureGraphBuilder(defaultGraphBuilder)
    val graph = configuredGraphBuilder.build
    val graphSystem = graph.asInstanceOf[PrivateGraph].getCoordinatorActorSystem
    logger = changeLogger(Some(graphSystem))
    graph.awaitIdle
    afterGraphBuilt

    val loadedGraph = loadGraph(graph)
    loadedGraph.awaitIdle
    afterGraphLoaded

    val executionResult = execute(loadedGraph)

    reportResults(executionResult._1, executionResult._2)
    tearDown(executionResult._2)
  }

  /**
   * this method makes it possible to change the logger dynamically
   */
  def changeLogger(system: Option[ActorSystem]): SimpleLogger = {
    if (system.isDefined && !system.get.isTerminated) {
      new SimpleAkkaLogger(system.get, this.getClass.getName)
    } else {
      SimpleConsoleLogger
    }
  }

  /**
   * is called before the execution begins
   */
  def beforeStart {}

  /**
   * can be overridden to configure the GraphBuilder to be used.
   * Per default it gives back the untouched GraphBuilder, which is passed in.
   */
  def configureGraphBuilder(gb: GraphBuilder[Any, Any]): GraphBuilder[Any, Any] = gb

  /**
   * this method is called when the graph is built
   */
  def afterGraphBuilt {}

  /**
   * must be implemented to load vertices and edges into the graph.
   */
  def loadGraph(g: Graph[Any, Any]): Graph[Any, Any]

  /**
   * this method is called when the graph is loaded,
   */
  def afterGraphLoaded {}

  /**
   * parameters for the Algorithm as a Map
   * per default this is an empty Map
   */
  def parameters: Map[String, String] = HashMap[String, String]()

  /**
   * default implementation for the execution
   */
  def execute(g: Graph[Any, Any]): (ExecutionInformation, Graph[Any, Any]) = {
    val stats = g.execute
    (stats, g)
  }

  /**
   * default implementation of the reporting, prints out stats to console
   */
  def reportResults(stats: ExecutionInformation, graph: Graph[Any, Any]) = log.info(stats)

  /**
   * default implementation of the teardown,
   * shutsdown the graph
   */
  def tearDown(g: Graph[Any, Any]) = g.shutdown

  /**
   *
   * this function should be called by an implementation of a Cluster (like in the @see com.signalcollect.deployment.DefaultLeader)
   * it creates a GraphBuilder with the nodeActors and ActorSystem if they exist.
   * @param nodeActors are the nodeActors provided
   * @param actorSystem if already an actorSystem is defined, it should be passed in, so that not a new one has to be created
   */
  def createDefaultGraphBuilder(nodeActors: Option[Array[ActorRef]],
    actorSystem: Option[ActorSystem] = None,
    graphBuilder: GraphBuilder[Any, Any] = GraphBuilder): GraphBuilder[Any, Any] = {
    val g1 = if (actorSystem.isDefined)
      graphBuilder.withActorSystem(actorSystem.get)
    else graphBuilder
    val g2 = if (nodeActors.isDefined)
      g1.withPreallocatedNodes(nodeActors.get)
    else
      g1
    g2
  }

}
