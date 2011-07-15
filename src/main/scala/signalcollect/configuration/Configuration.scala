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

package signalcollect.configuration

import signalcollect.configuration.bootstrap._
import signalcollect.interfaces._
import java.util.HashMap

object DefaultConfiguration extends Configuration

/**
 * Main configuration for Signal Collect. Used for constructing a [compute graph]
 */
case class Configuration(
    numberOfWorkers: Int = Runtime.getRuntime.availableProcessors,
    customLogger: Option[MessageRecipient[LogMessage]] = None,
    graphConfiguration: GraphConfiguration = DefaultGraphConfiguration,
    bootstrapConfiguration: BootstrapConfiguration = DefaultBootstrapConfiguration,
    workerConfigurations: HashMap[Int, WorkerConfiguration] = new HashMap[Int, WorkerConfiguration]()
    )