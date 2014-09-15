/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
 *
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.factory.mapper

import com.signalcollect.interfaces.MapperFactory
import com.signalcollect.interfaces.VertexToWorkerMapper
import com.signalcollect.messaging.DefaultVertexToWorkerMapper

/**
 *  Default random hash partitioning mapper.
 *  Has good load balancing but poor locality.
 */
class DefaultMapperFactory[@specialized(Int, Long) Id] extends MapperFactory[Id] {
  def createInstance(numberOfNodes: Int, workersPerNode: Int): VertexToWorkerMapper[Id] =
    new DefaultVertexToWorkerMapper[Id](numberOfNodes, workersPerNode)
  override def toString = "DefaultMapperFactory"
}
