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

package com.signalcollect.javaapi

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.configuration._

/**
 *  An execution configuration specifies execution parameters for a computation. This object
 *  represents an ExecutionConfiguration that is initialized with the default parameters.
 */
object ExecutionConfiguration extends ExecutionConfiguration(ExecutionMode.OptimizedAsynchronous, 0.01, 0.0, None, None)