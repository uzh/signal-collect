/*
 *  @author Philip Stutz
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

package com.signalcollect.features

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.signalcollect._
import com.signalcollect.interfaces._
import com.signalcollect.graphproviders._
import com.signalcollect.examples._
import com.signalcollect.configuration._

trait SpecConfigurations {

  def computeGraphBuilders = List(GraphBuilder)
  def numberOfWorkers = List(1, 2, 4, 8, 16, 32, 64, 128)
  def executionModes = List(ExecutionMode.OptimizedAsynchronous, ExecutionMode.Synchronous)

  def computeGraphs: Seq[Graph] = {
    var computeGraphs = Seq[Graph]()
    for (workers <- numberOfWorkers) {
      for (computeGraphBuilder <- computeGraphBuilders) {
        computeGraphs = computeGraphBuilder.build +: computeGraphs
      }
    }
    computeGraphs
  }

  def executionConfigurations: Seq[ExecutionConfiguration] = {
    var executionConfigurations = Seq[ExecutionConfiguration]()
    for (executionMode <- executionModes) {
      executionConfigurations = ExecutionConfiguration(executionMode = executionMode) +: executionConfigurations
    }
    executionConfigurations
  }

}